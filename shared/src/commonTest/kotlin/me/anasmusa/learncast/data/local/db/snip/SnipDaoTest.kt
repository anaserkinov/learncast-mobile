package me.anasmusa.learncast.data.local.db.snip

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Outbox.createOutboxEntity
import me.anasmusa.learncast.data.local.db.TestFixtures.Snip.createSnip
import me.anasmusa.learncast.data.local.db.TestFixtures.Snip.createSnips
import me.anasmusa.learncast.data.local.db.TestFixtures.loadList
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.outbox.OutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.SnipOutboxEntity
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.OutboxStatus
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.ReferenceType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class SnipDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var snipDao: SnipDao
    private lateinit var outboxDao: OutboxDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        snipDao = database.getSnipDao()
        outboxDao = database.getOutboxDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    // ========== Insert Tests ==========

    @Test
    fun insertSingleItemShouldAddSnipToDatabase() = runTest {
        // Given
        val snip = createSnip(clientSnipId = "snip-1", id = 1)

        // When
        snipDao.insert(snip)

        // Then
        val result = snipDao.getByClientSnipId("snip-1")
        assertNotNull(result)
        assertEquals("snip-1", result.clientSnipId)
        assertEquals(1L, result.id)
    }

    @Test
    fun insertMultipleItemsShouldAddAllSnips() = runTest {
        // Given
        val snips = createSnips(3)

        // When
        snipDao.insert(snips)

        // Then
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()
        assertEquals(3, result.size)
    }

    @Test
    fun insertWithOnConflictStrategyReplaceShouldUpdateExistingSnip() = runTest {
        // Given
        val originalSnip = createSnip(clientSnipId = "snip-1", id = 1, note = "Original note")
        snipDao.insert(originalSnip)

        // When
        val updatedSnip = createSnip(clientSnipId = "snip-1", id = 1, note = "Updated note")
        snipDao.insert(updatedSnip)

        // Then
        val result = snipDao.getByClientSnipId("snip-1")
        assertNotNull(result)
        assertEquals("Updated note", result.note)
    }

    @Test
    fun insertEmptyListShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        snipDao.insert(emptyList())
    }

    // ========== Delete Tests ==========

    @Test
    fun deleteByClientSnipIdShouldRemoveSnip() = runTest {
        // Given
        val snips = createSnips(3)
        snipDao.insert(snips)

        // When
        snipDao.delete("snip-2")

        // Then
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()
        assertEquals(2, result.size)
        assertTrue(result.none { it.clientSnipId == "snip-2" })
    }

    @Test
    fun deleteByClientSnipIdWithNonExistentIdShouldNotFail() = runTest {
        // Given
        val snips = createSnips(2)
        snipDao.insert(snips)

        // When/Then - Should not throw exception
        snipDao.delete("non-existent-snip")

        // Then
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()
        assertEquals(2, result.size)
    }

    @Test
    fun deleteByIdsShouldRemoveSpecifiedSnips() = runTest {
        // Given
        val snips = createSnips(5)
        snipDao.insert(snips)

        // When
        snipDao.delete(listOf(2L, 4L))

        // Then
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()
        assertEquals(3, result.size)
        assertTrue(result.none { it.id == 2L || it.id == 4L })
    }

    @Test
    fun deleteByIdsWithEmptyListShouldNotFail() = runTest {
        // Given
        val snips = createSnips(2)
        snipDao.insert(snips)

        // When
        snipDao.delete(emptyList())

        // Then
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()
        assertEquals(2, result.size)
    }

    // ========== getByClientSnipId Tests ==========

    @Test
    fun getByClientSnipIdShouldReturnSnip() = runTest {
        // Given
        val snip = createSnip(clientSnipId = "snip-test", id = 1, note = "Test note")
        snipDao.insert(snip)

        // When
        val result = snipDao.getByClientSnipId("snip-test")

        // Then
        assertNotNull(result)
        assertEquals("snip-test", result.clientSnipId)
        assertEquals("Test note", result.note)
    }

    @Test
    fun getByClientSnipIdWithNonExistentIdShouldReturnNull() = runTest {
        // When
        val result = snipDao.getByClientSnipId("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun getByClientSnipIdShouldCoalesceFromSnipOutbox() = runTest {
        // Given
        val snip = createSnip(
            clientSnipId = "snip-1",
            id = 1,
            startMs = 1000,
            endMs = 5000,
            note = "Original note"
        )
        snipDao.insert(snip)

        // Insert SnipOutbox with updated values
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "snip-1",
            referenceType = ReferenceType.SNIP,
            actionType = ActionType.UPDATE,
            createdAt = now
        )
        val outboxId = outboxDao.insert(outboxEntry)

        val snipOutbox = SnipOutboxEntity(
            id = 0,
            outboxId = outboxId,
            clientSnipId = "snip-1",
            lessonId = 1,
            startMs = 2000,  // Updated
            endMs = 6000,    // Updated
            note = "Updated note"  // Updated
        )
        outboxDao.insert(snipOutbox)

        // When
        val result = snipDao.getByClientSnipId("snip-1")

        // Then - Should use SnipOutbox values
        assertNotNull(result)
        assertEquals(2000L, result.startMs)
        assertEquals(6000L, result.endMs)
        assertEquals("Updated note", result.note)
    }

    @Test
    fun getByClientSnipIdShouldUseSnipValuesWhenNoOutbox() = runTest {
        // Given
        val snip = createSnip(
            clientSnipId = "snip-1",
            id = 1,
            startMs = 1000,
            endMs = 5000,
            note = "Original note"
        )
        snipDao.insert(snip)

        // No SnipOutbox entry

        // When
        val result = snipDao.getByClientSnipId("snip-1")

        // Then - Should use original Snip values
        assertNotNull(result)
        assertEquals(1000L, result.startMs)
        assertEquals(5000L, result.endMs)
        assertEquals("Original note", result.note)
    }

    // ========== getSnips Filter Tests ==========

    @Test
    fun getSnipsWithNullFiltersShouldReturnAllSnips() = runTest {
        // Given
        val snips = createSnips(3)
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun getSnipsWithBlankSearchShouldReturnAllSnips() = runTest {
        // Given
        val snips = createSnips(2)
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = "  ",
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getSnipsWithSearchShouldFilterByNote() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, note = "Important concept about Kotlin"),
            createSnip(clientSnipId = "snip-2", id = 2, note = "Java best practices"),
            createSnip(clientSnipId = "snip-3", id = 3, note = "Kotlin coroutines tutorial")
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = "Kotlin",
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.note?.contains("Kotlin", ignoreCase = true) == true })
    }

    @Test
    fun getSnipsWithSearchShouldBeCaseInsensitive() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, note = "IMPORTANT NOTE"),
            createSnip(clientSnipId = "snip-2", id = 2, note = "important note")
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = "important",
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getSnipsWithAuthorIdShouldFilterByAuthor() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, authorId = 1),
            createSnip(clientSnipId = "snip-2", id = 2, authorId = 1),
            createSnip(clientSnipId = "snip-3", id = 3, authorId = 2)
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = 1,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.authorId == 1L })
    }

    @Test
    fun getSnipsWithTopicIdShouldFilterByTopic() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, topicId = 10),
            createSnip(clientSnipId = "snip-2", id = 2, topicId = 10),
            createSnip(clientSnipId = "snip-3", id = 3, topicId = 20)
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = 10,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.topicId == 10L })
    }

    @Test
    fun getSnipsWithLessonIdShouldFilterByLesson() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, lessonId = 100),
            createSnip(clientSnipId = "snip-2", id = 2, lessonId = 100),
            createSnip(clientSnipId = "snip-3", id = 3, lessonId = 200)
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = 100,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.lessonId == 100L })
    }

    @Test
    fun getSnipsWithMultipleFiltersShouldApplyAll() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, note = "Kotlin tip", authorId = 1, topicId = 10, lessonId = 100),
            createSnip(clientSnipId = "snip-2", id = 2, note = "Kotlin trick", authorId = 1, topicId = 10, lessonId = 100),
            createSnip(clientSnipId = "snip-3", id = 3, note = "Java tip", authorId = 1, topicId = 10, lessonId = 100),
            createSnip(clientSnipId = "snip-4", id = 4, note = "Kotlin example", authorId = 2, topicId = 10, lessonId = 100)
        )
        snipDao.insert(snips)

        // When - Filter by search + authorId + topicId + lessonId
        val result = snipDao.getSnips(
            search = "Kotlin",
            authorId = 1,
            topicId = 10,
            lessonId = 100,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { 
            it.note?.contains("Kotlin", ignoreCase = true) == true &&
            it.authorId == 1L &&
            it.topicId == 10L &&
            it.lessonId == 100L
        })
    }

    // ========== getSnips Sorting Tests ==========

    @Test
    fun getSnipsWithSortByCreatedAtAscShouldOrderCorrectly() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, createdAt = LocalDateTime(2024, 1, 3, 0, 0, 0)),
            createSnip(clientSnipId = "snip-2", id = 2, createdAt = LocalDateTime(2024, 1, 1, 0, 0, 0)),
            createSnip(clientSnipId = "snip-3", id = 3, createdAt = LocalDateTime(2024, 1, 2, 0, 0, 0))
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = QuerySort.CREATED_AT,
            order = QueryOrder.ASC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].id) // Earliest
        assertEquals(3L, result[1].id)
        assertEquals(1L, result[2].id) // Latest
    }

    @Test
    fun getSnipsWithSortByCreatedAtDescShouldOrderCorrectly() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, createdAt = LocalDateTime(2024, 1, 3, 0, 0, 0)),
            createSnip(clientSnipId = "snip-2", id = 2, createdAt = LocalDateTime(2024, 1, 1, 0, 0, 0)),
            createSnip(clientSnipId = "snip-3", id = 3, createdAt = LocalDateTime(2024, 1, 2, 0, 0, 0))
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = QuerySort.CREATED_AT,
            order = QueryOrder.DESC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(1L, result[0].id) // Latest
        assertEquals(3L, result[1].id)
        assertEquals(2L, result[2].id) // Earliest
    }

    // ========== getSnips Integration Tests (OUTBOX) ==========

    @Test
    fun getSnipsShouldExcludeSnipsWithDeleteAction() = runTest {
        // Given
        val snips = createSnips(3)
        snipDao.insert(snips)

        // Mark snip-2 for deletion in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val deleteAction = createOutboxEntity(
            id = 1,
            referenceId = 2,
            referenceUuid = "snip-2",
            referenceType = ReferenceType.SNIP,
            actionType = ActionType.DELETE,
            createdAt = now
        )
        outboxDao.insert(deleteAction)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then - Should exclude snip-2
        assertEquals(2, result.size)
        assertTrue(result.none { it.clientSnipId == "snip-2" })
    }

    @Test
    fun getSnipsShouldIncludeSnipsWithNonDeleteActions() = runTest {
        // Given
        val snips = createSnips(2)
        snipDao.insert(snips)

        // Add CREATE action (should not exclude)
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val createAction = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "snip-1",
            referenceType = ReferenceType.SNIP,
            actionType = ActionType.CREATE,
            createdAt = now
        )
        outboxDao.insert(createAction)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then - Should include both snips
        assertEquals(2, result.size)
    }

    @Test
    fun getSnipsShouldCoalesceValuesFromSnipOutbox() = runTest {
        // Given
        val snip = createSnip(
            clientSnipId = "snip-1",
            id = 1,
            startMs = 1000,
            endMs = 5000,
            note = "Original note"
        )
        snipDao.insert(snip)

        // Add SnipOutbox with updated values
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "snip-1",
            referenceType = ReferenceType.SNIP,
            actionType = ActionType.UPDATE,
            createdAt = now
        )
        val outboxId = outboxDao.insert(outboxEntry)

        val snipOutbox = SnipOutboxEntity(
            id = 0,
            outboxId = outboxId,
            clientSnipId = "snip-1",
            lessonId = 1,
            startMs = 3000,  // Updated
            endMs = 8000,    // Updated
            note = "Updated note"  // Updated
        )
        outboxDao.insert(snipOutbox)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then - Should use SnipOutbox values via COALESCE
        assertEquals(1, result.size)
        assertEquals(3000L, result[0].startMs)
        assertEquals(8000L, result[0].endMs)
        assertEquals("Updated note", result[0].note)
    }

    @Test
    fun getSnipsShouldUseOriginalValuesWhenNoSnipOutbox() = runTest {
        // Given
        val snip = createSnip(
            clientSnipId = "snip-1",
            id = 1,
            startMs = 1000,
            endMs = 5000,
            note = "Original note"
        )
        snipDao.insert(snip)

        // No SnipOutbox entry

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then - Should use original values
        assertEquals(1, result.size)
        assertEquals(1000L, result[0].startMs)
        assertEquals(5000L, result[0].endMs)
        assertEquals("Original note", result[0].note)
    }

    // ========== Edge Cases ==========

    @Test
    fun getSnipsShouldReturnEmptyListWhenNoMatchesFound() = runTest {
        // Given
        val snips = createSnips(2)
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = "NonexistentNote",
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getSnipsShouldHandleNullNote() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, note = null),
            createSnip(clientSnipId = "snip-2", id = 2, note = "Has note")
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertNull(result.find { it.clientSnipId == "snip-1" }?.note)
        assertNotNull(result.find { it.clientSnipId == "snip-2" }?.note)
    }

    @Test
    fun getSnipsShouldHandleNullableTopicId() = runTest {
        // Given
        val snips = listOf(
            createSnip(clientSnipId = "snip-1", id = 1, topicId = null),
            createSnip(clientSnipId = "snip-2", id = 2, topicId = 10)
        )
        snipDao.insert(snips)

        // When
        val result = snipDao.getSnips(
            search = null,
            authorId = null,
            topicId = null,
            lessonId = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }
}
