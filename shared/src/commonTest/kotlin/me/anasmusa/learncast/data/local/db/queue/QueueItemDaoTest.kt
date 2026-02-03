package me.anasmusa.learncast.data.local.db.queue

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Queue.createQueueItem
import me.anasmusa.learncast.data.local.db.TestFixtures.Queue.createQueueItems
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import me.anasmusa.learncast.data.local.db.lesson.LessonDao
import me.anasmusa.learncast.data.local.db.lesson.LessonStateInput
import me.anasmusa.learncast.data.local.db.outbox.LessonOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class QueueItemDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var queueDao: QueueItemDao
    private lateinit var lessonDao: LessonDao
    private lateinit var downloadDao: DownloadDao
    private lateinit var outboxDao: OutboxDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        queueDao = database.getQueueItemDao()
        lessonDao = database.getLessonDao()
        downloadDao = database.getDownloadDao()
        outboxDao = database.getOutboxDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    // ========== Basic CRUD Tests ==========

    @Test
    fun insertSingleItemShouldReturnId() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0)

        // When
        val insertedId = queueDao.insert(item)

        // Then
        assertTrue(insertedId > 0)
        val result = queueDao.getById(insertedId)
        assertNotNull(result)
    }

    @Test
    fun insertMultipleItemsShouldAddAll() = runTest {
        // Given
        val items = createQueueItems(3)

        // When
        queueDao.insert(items)

        // Then
        assertEquals(3, queueDao.count())
    }

    @Test
    fun deleteByIdShouldRemoveItem() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0)
        val insertedId = queueDao.insert(item)

        // When
        queueDao.delete(insertedId)

        // Then
        assertNull(queueDao.getById(insertedId))
        assertEquals(0, queueDao.count())
    }

    @Test
    fun clearShouldRemoveAllItems() = runTest {
        // Given
        val items = createQueueItems(5)
        queueDao.insert(items)

        // When
        queueDao.clear()

        // Then
        assertEquals(0, queueDao.count())
    }

    @Test
    fun clearExceptFirstShouldKeepOnlyFirstItem() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)

        // When
        queueDao.clearExceptFirst()

        // Then
        assertEquals(1, queueDao.count())
        val remaining = queueDao.getByOrder(0)
        assertNotNull(remaining)
        assertEquals(0, remaining.order)
    }

    @Test
    fun countShouldReturnCorrectNumber() = runTest {
        // Given
        val items = createQueueItems(7)
        queueDao.insert(items)

        // When
        val count = queueDao.count()

        // Then
        assertEquals(7, count)
    }

    @Test
    fun getByIdShouldReturnCorrectItem() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0, title = "Test Item")
        val insertedId = queueDao.insert(item)

        // When
        val result = queueDao.getById(insertedId)

        // Then
        assertNotNull(result)
        assertEquals(insertedId, result.id)
        assertEquals("Test Item", result.title)
    }

    @Test
    fun getByOrderShouldReturnCorrectItem() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)

        // When
        val result = queueDao.getByOrder(1)

        // Then
        assertNotNull(result)
        assertEquals(1, result.order)
    }

    // ========== Order Management: moveBy Tests ==========

    @Test
    fun moveByPositiveShouldShiftAllItemsDown() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)

        // When
        queueDao.moveBy(1) // Shift all by +1

        // Then
        assertNotNull(queueDao.getByOrder(1)) // Was 0
        assertNotNull(queueDao.getByOrder(2)) // Was 1
        assertNotNull(queueDao.getByOrder(3)) // Was 2
        assertNull(queueDao.getByOrder(0))
    }

    @Test
    fun moveByNegativeShouldShiftAllItemsUp() = runTest {
        // Given
        val items = listOf(
            createQueueItem(id = 0, order = 1),
            createQueueItem(id = 0, order = 2),
            createQueueItem(id = 0, order = 3)
        )
        queueDao.insert(items)

        // When
        queueDao.moveBy(-1) // Shift all by -1

        // Then
        assertNotNull(queueDao.getByOrder(0)) // Was 1
        assertNotNull(queueDao.getByOrder(1)) // Was 2
        assertNotNull(queueDao.getByOrder(2)) // Was 3
        assertNull(queueDao.getByOrder(3))
    }

    @Test
    fun moveByRangeShouldShiftOnlySpecifiedRange() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)

        // When - Shift items 1-3 by +2
        queueDao.moveBy(from = 1, to = 3, by = 2)

        // Then
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNull(queueDao.getByOrder(1))    // Was shifted
        assertNull(queueDao.getByOrder(2))    // Was shifted
        assertNotNull(queueDao.getByOrder(3)) // Now has item from order 1
        assertNotNull(queueDao.getByOrder(4)) // Unchanged
        assertNotNull(queueDao.getByOrder(5)) // Now has item from order 3
    }

    @Test
    fun moveByAfterShouldShiftItemsAfterSpecificId() = runTest {
        // Given
        val items = createQueueItems(4) // orders: 0, 1, 2, 3
        queueDao.insert(items)
        val item2 = queueDao.getByOrder(2)!!

        // When - Shift all items >= order 2 by +1
        queueDao.moveByAfter(id = item2.id, by = 1)

        // Then
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNotNull(queueDao.getByOrder(1)) // Unchanged
        assertNull(queueDao.getByOrder(2))    // Shifted to 3
        assertNotNull(queueDao.getByOrder(3)) // Was 2
        assertNotNull(queueDao.getByOrder(4)) // Was 3
    }

    @Test
    fun moveToShouldMoveItemToSpecificOrder() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 5, title = "Target Item")
        val insertedId = queueDao.insert(item)

        // When
        queueDao.moveTo(id = insertedId, to = 10)

        // Then
        val result = queueDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(10, result.order)
    }

    // ========== Order Management: move() Tests (CRITICAL) ==========

    @Test
    fun moveShouldMoveItemFromStartToEnd() = runTest {
        // Given
        val items = createQueueItems(4) // orders: 0, 1, 2, 3
        queueDao.insert(items)
        val originalFirst = queueDao.getByOrder(0)!!

        // When - Move first (0) to last (3)
        queueDao.move(from = 0, to = 3)

        // Then
        val result = queueDao.getById(originalFirst.id)
        assertNotNull(result)
        assertEquals(3, result.order) // Now at position 3
        
        // Others should shift up
        assertNotNull(queueDao.getByOrder(0)) // Was 1
        assertNotNull(queueDao.getByOrder(1)) // Was 2
        assertNotNull(queueDao.getByOrder(2)) // Was 3
    }

    @Test
    fun moveShouldMoveItemFromEndToStart() = runTest {
        // Given
        val items = createQueueItems(4) // orders: 0, 1, 2, 3
        queueDao.insert(items)
        val originalLast = queueDao.getByOrder(3)!!

        // When - Move last (3) to first (0)
        queueDao.move(from = 3, to = 0)

        // Then
        val result = queueDao.getById(originalLast.id)
        assertNotNull(result)
        assertEquals(0, result.order) // Now at position 0
        
        // Others should shift down
        assertNotNull(queueDao.getByOrder(1)) // Was 0
        assertNotNull(queueDao.getByOrder(2)) // Was 1
        assertNotNull(queueDao.getByOrder(3)) // Was 2
    }

    @Test
    fun moveShouldMoveItemForward() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)
        val item1 = queueDao.getByOrder(1)!!

        // When - Move item at position 1 to position 3
        queueDao.move(from = 1, to = 3)

        // Then
        val result = queueDao.getById(item1.id)
        assertEquals(3, result!!.order)
        
        // Items 2 and 3 should shift up to 1 and 2
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNotNull(queueDao.getByOrder(1)) // Was 2
        assertNotNull(queueDao.getByOrder(2)) // Was 3
        assertNotNull(queueDao.getByOrder(4)) // Unchanged
    }

    @Test
    fun moveShouldMoveItemBackward() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)
        val item3 = queueDao.getByOrder(3)!!

        // When - Move item at position 3 to position 1
        queueDao.move(from = 3, to = 1)

        // Then
        val result = queueDao.getById(item3.id)
        assertEquals(1, result!!.order)
        
        // Items 1 and 2 should shift down to 2 and 3
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNotNull(queueDao.getByOrder(2)) // Was 1
        assertNotNull(queueDao.getByOrder(3)) // Was 2
        assertNotNull(queueDao.getByOrder(4)) // Unchanged
    }

    @Test
    fun moveShouldHandleSamePosition() = runTest {
        // Given
        val items = createQueueItems(3)
        queueDao.insert(items)

        // When - Move item to its own position
        queueDao.move(from = 1, to = 1)

        // Then - Nothing should change
        assertNotNull(queueDao.getByOrder(0))
        assertNotNull(queueDao.getByOrder(1))
        assertNotNull(queueDao.getByOrder(2))
    }

    // ========== Order Management: remove() Tests (CRITICAL) ==========

    @Test
    fun removeShouldDeleteItemAndShiftOthersUp() = runTest {
        // Given
        val items = createQueueItems(4) // orders: 0, 1, 2, 3
        queueDao.insert(items)
        val item1 = queueDao.getByOrder(1)!!

        // When - Remove item at position 1
        queueDao.remove(item1.id)

        // Then
        assertNull(queueDao.getById(item1.id)) // Item deleted
        assertEquals(3, queueDao.count()) // Count reduced
        
        // Items after should shift up
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNotNull(queueDao.getByOrder(1)) // Was 2
        assertNotNull(queueDao.getByOrder(2)) // Was 3
        assertNull(queueDao.getByOrder(3))    // Gap removed
    }

    @Test
    fun removeShouldHandleFirstItem() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)
        val first = queueDao.getByOrder(0)!!

        // When
        queueDao.remove(first.id)

        // Then
        assertEquals(2, queueDao.count())
        assertNotNull(queueDao.getByOrder(0)) // Was 1
        assertNotNull(queueDao.getByOrder(1)) // Was 2
    }

    @Test
    fun removeShouldHandleLastItem() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)
        val last = queueDao.getByOrder(2)!!

        // When
        queueDao.remove(last.id)

        // Then
        assertEquals(2, queueDao.count())
        assertNotNull(queueDao.getByOrder(0)) // Unchanged
        assertNotNull(queueDao.getByOrder(1)) // Unchanged
        assertNull(queueDao.getByOrder(2))    // Removed
    }

    // ========== Order Management: addFirst() Tests (CRITICAL) ==========

    @Test
    fun addFirstShouldInsertAtPositionZero() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)

        // When
        val newItem = createQueueItem(id = 0, order = 0, title = "New First")
        val insertedId = queueDao.addFirst(newItem)

        // Then
        val result = queueDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(0, result.order)
        assertEquals("New First", result.title)
        assertEquals(4, queueDao.count())
    }

    @Test
    fun addFirstShouldShiftExistingItemsDown() = runTest {
        // Given
        val items = createQueueItems(3) // orders: 0, 1, 2
        queueDao.insert(items)

        // When
        val newItem = createQueueItem(id = 0, order = 0, title = "New First")
        queueDao.addFirst(newItem)

        // Then - All old items should shift down by 1
        assertNotNull(queueDao.getByOrder(0)) // New item
        assertNotNull(queueDao.getByOrder(1)) // Was 0
        assertNotNull(queueDao.getByOrder(2)) // Was 1
        assertNotNull(queueDao.getByOrder(3)) // Was 2
    }

    @Test
    fun addFirstToEmptyQueueShouldWork() = runTest {
        // Given - Empty queue

        // When
        val newItem = createQueueItem(id = 0, order = 0, title = "Only Item")
        val insertedId = queueDao.addFirst(newItem)

        // Then
        assertEquals(1, queueDao.count())
        val result = queueDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(0, result.order)
    }

    // ========== Order Management: ensureItemIsFirst() Tests (CRITICAL) ==========

    @Test
    fun ensureItemIsFirstShouldMoveItemToFirstAndDeleteBefore() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)
        val item2 = queueDao.getByOrder(2)!!

        // When
        queueDao.ensureItemIsFirst(item2.id)

        // Then
        val result = queueDao.getById(item2.id)
        assertEquals(0, result!!.order) // Now at position 0
        assertEquals(3, queueDao.count()) // Items 0 and 1 were deleted
        
        // Only items 2, 3, 4 remain (now at 0, 1, 2)
        assertNotNull(queueDao.getByOrder(0))
        assertNotNull(queueDao.getByOrder(1))
        assertNotNull(queueDao.getByOrder(2))
    }

    @Test
    fun ensureItemIsFirstWhenAlreadyFirstShouldDoNothing() = runTest {
        // Given
        val items = createQueueItems(3)
        queueDao.insert(items)
        val first = queueDao.getByOrder(0)!!

        // When
        queueDao.ensureItemIsFirst(first.id)

        // Then
        assertEquals(3, queueDao.count()) // No items deleted
        assertEquals(0, queueDao.getByOrder(0)!!.order)
    }

    @Test
    fun ensureItemIsFirstShouldDeleteAllBefore() = runTest {
        // Given
        val items = createQueueItems(10) // orders: 0-9
        queueDao.insert(items)
        val item7 = queueDao.getByOrder(7)!!

        // When
        queueDao.ensureItemIsFirst(item7.id)

        // Then
        assertEquals(3, queueDao.count()) // Only 7, 8, 9 remain
        assertEquals(0, queueDao.getById(item7.id)!!.order)
    }

    // ========== Order Management: replace() Tests ==========

    @Test
    fun replaceShouldClearAndInsertNewItems() = runTest {
        // Given
        val oldItems = createQueueItems(3)
        queueDao.insert(oldItems)

        // When
        val newItems = listOf(
            createQueueItem(id = 0, order = 0, title = "New 1"),
            createQueueItem(id = 0, order = 1, title = "New 2")
        )
        queueDao.replace(newItems)

        // Then
        assertEquals(2, queueDao.count())
        assertEquals("New 1", queueDao.getByOrder(0)?.title)
        assertEquals("New 2", queueDao.getByOrder(1)?.title)
    }

    @Test
    fun replaceWithEmptyListShouldClearQueue() = runTest {
        // Given
        val items = createQueueItems(5)
        queueDao.insert(items)

        // When
        queueDao.replace(emptyList())

        // Then
        assertEquals(0, queueDao.count())
    }

    // ========== Update Tests ==========

    @Test
    fun updateSnipQueueItemShouldUpdateFields() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceType = ReferenceType.SNIP,
            startMs = 1000,
            endMs = 5000,
            title = "Old Title"
        )
        val insertedId = queueDao.insert(item)

        // When
        queueDao.updateSnipQueueItem(
            id = insertedId,
            startMs = 2000,
            endMs = 6000,
            title = "New Title"
        )

        // Then
        val result = queueDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(2000L, result.startMs)
        assertEquals(6000L, result.endMs)
        assertEquals("New Title", result.title)
    }

    @Test
    fun deleteBeforeShouldRemoveItemsBeforeSpecifiedId() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)
        val item3 = queueDao.getByOrder(3)!!

        // When
        val deletedCount = queueDao.deleteBefore(item3.id)

        // Then
        assertEquals(3, deletedCount) // Items 0, 1, 2 deleted
        assertEquals(2, queueDao.count()) // Only 3, 4 remain
    }

    @Test
    fun getLessonIdShouldReturnLessonIdForLessonType() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceType = ReferenceType.LESSON,
            lessonId = 42
        )
        val insertedId = queueDao.insert(item)

        // When
        val lessonId = queueDao.getLessonId(insertedId)

        // Then
        assertEquals(42L, lessonId)
    }

    @Test
    fun getLessonIdShouldReturnNullForSnipType() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceType = ReferenceType.SNIP,
            lessonId = 42
        )
        val insertedId = queueDao.insert(item)

        // When
        val lessonId = queueDao.getLessonId(insertedId)

        // Then
        assertNull(lessonId)
    }

    // ========== View Integration Tests ==========

    @Test
    fun getWithStateByIdShouldReturnItemWithState() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0, lessonId = 1)
        val insertedId = queueDao.insert(item)

        // Setup lesson state
        val lessonState = LessonStateInput(
            lessonId = 1,
            listenCount = 5,
            snipCount = 10,
            isFavourite = true,
            startedAt = null,
            lastPositionMs = 30.seconds,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = null
        )
        lessonDao.upsertStates(listOf(lessonState))

        // When
        val result = queueDao.getWithStateById(insertedId)

        // Then
        assertNotNull(result)
        assertEquals(insertedId, result.item.id)
        assertEquals(1L, result.state.lessonId)
        assertEquals(5L, result.state.listenCount)
        assertEquals(10L, result.state.snipCount)
        assertTrue(result.state.isFavourite)
    }

    @Test
    fun getWithStateByIdShouldHandleMissingState() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0, lessonId = 1)
        val insertedId = queueDao.insert(item)

        // No lesson state setup

        // When
        val result = queueDao.getWithStateById(insertedId)

        // Then - View should still return with default values via COALESCE
        assertNotNull(result)
        assertEquals(insertedId, result.item.id)
        assertEquals(0L, result.state.listenCount) // Default from COALESCE
        assertEquals(UserProgressStatus.NOT_STARTED, result.state.status) // Default
    }

    @Test
    fun getByLessonReferenceIdShouldReturnItem() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceType = ReferenceType.LESSON,
            referenceId = 100,
            lessonId = 100
        )
        queueDao.insert(item)

        // When
        val result = queueDao.getByLessonReferenceId(100)

        // Then
        assertNotNull(result)
        assertEquals(100L, result.item.referenceId)
        assertEquals(ReferenceType.LESSON, result.item.referenceType)
    }

    @Test
    fun getBySnipReferenceUuidShouldReturnItem() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceType = ReferenceType.SNIP,
            referenceUuid = "snip-uuid-123"
        )
        queueDao.insert(item)

        // When
        val result = queueDao.getBySnipReferenceUuid("snip-uuid-123")

        // Then
        assertNotNull(result)
        assertEquals("snip-uuid-123", result.item.referenceUuid)
        assertEquals(ReferenceType.SNIP, result.item.referenceType)
    }

    @Test
    fun getAllShouldReturnItemsInOrder() = runTest {
        // Given
        val items = createQueueItems(5) // orders: 0, 1, 2, 3, 4
        queueDao.insert(items)

        // When
        val result = queueDao.getAll()

        // Then
        assertEquals(5, result.size)
        assertEquals(0, result[0].item.order)
        assertEquals(1, result[1].item.order)
        assertEquals(2, result[2].item.order)
        assertEquals(3, result[3].item.order)
        assertEquals(4, result[4].item.order)
    }

    @Test
    fun getWithStateByIdShouldIncludeDownloadState() = runTest {
        // Given
        val item = createQueueItem(
            id = 0,
            order = 0,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            lessonId = 1
        )
        val insertedId = queueDao.insert(item)

        // Add download state
        val downloadState = DownloadStateEntity(
            id = 0,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            audioPath = "/audio.mp3",
            startMs = null,
            endMs = null,
            state = DownloadState.COMPLETED,
            percentDownloaded = 100f
        )
        downloadDao.insert(downloadState)

        // When
        val result = queueDao.getWithStateById(insertedId)

        // Then
        assertNotNull(result)
        assertEquals(DownloadState.COMPLETED, result.downloadState)
        assertEquals(100f, result.percentDownloaded)
    }

    @Test
    fun getWithStateByIdShouldCoalesceFavouriteFromOutbox() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0, lessonId = 1, referenceId = 1)
        val insertedId = queueDao.insert(item)

        // Setup lesson state with isFavourite = false
        val lessonState = LessonStateInput(
            lessonId = 1,
            listenCount = 0,
            snipCount = 0,
            isFavourite = false,
            startedAt = null,
            lastPositionMs = null,
            status = UserProgressStatus.NOT_STARTED,
            completedAt = null
        )
        lessonDao.upsertStates(listOf(lessonState))

        // Add FAVOURITE action in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = OutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.FAVOURITE,
            createdAt = now,
            updatedAt = now,
            lastTriedAt = null,
            status = OutboxStatus.PENDING
        )
        outboxDao.insert(outboxEntry)

        // When
        val result = queueDao.getWithStateById(insertedId)

        // Then - Should show favourite = true from OUTBOX
        assertNotNull(result)
        assertTrue(result.state.isFavourite)
    }

    @Test
    fun getWithStateByIdShouldCoalesceProgressFromLessonOutbox() = runTest {
        // Given
        val item = createQueueItem(id = 0, order = 0, lessonId = 1, referenceId = 1)
        val insertedId = queueDao.insert(item)

        // Setup lesson state
        val lessonState = LessonStateInput(
            lessonId = 1,
            listenCount = 0,
            snipCount = 0,
            isFavourite = false,
            startedAt = null,
            lastPositionMs = 10.seconds,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = null
        )
        lessonDao.upsertStates(listOf(lessonState))

        // Add LessonOutbox with updated progress
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = OutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.UPDATE,
            createdAt = now,
            updatedAt = now,
            lastTriedAt = null,
            status = OutboxStatus.PENDING
        )
        val outboxId = outboxDao.insert(outboxEntry)

        val lessonOutbox = LessonOutboxEntity(
            id = 0,
            outboxId = outboxId,
            lessonId = 1,
            startedAt = LocalDateTime(2024, 1, 1, 10, 0, 0),
            lastPositionMs = 120.seconds,
            status = UserProgressStatus.COMPLETED,
            completedAt = LocalDateTime(2024, 1, 1, 11, 0, 0)
        )
        outboxDao.insert(lessonOutbox)

        // When
        val result = queueDao.getWithStateById(insertedId)

        // Then - Should use LessonOutbox values via COALESCE
        assertNotNull(result)
        assertEquals(120.seconds, result.state.lastPositionMs)
        assertEquals(UserProgressStatus.COMPLETED, result.state.status)
    }

}
