package me.anasmusa.learncast.data.local.db.lesson

import io.kotest.core.annotation.Ignored
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Download.createDownloadState
import me.anasmusa.learncast.data.local.db.TestFixtures.Lesson.createLesson
import me.anasmusa.learncast.data.local.db.TestFixtures.Lesson.createLessonStateInput
import me.anasmusa.learncast.data.local.db.TestFixtures.Lesson.createLessons
import me.anasmusa.learncast.data.local.db.TestFixtures.Outbox.createOutboxEntity
import me.anasmusa.learncast.data.local.db.TestFixtures.Snip.createSnip
import me.anasmusa.learncast.data.local.db.TestFixtures.loadList
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import me.anasmusa.learncast.data.local.db.outbox.LessonOutboxEntity
import me.anasmusa.learncast.data.local.db.outbox.OutboxDao
import me.anasmusa.learncast.data.local.db.snip.SnipDao
import me.anasmusa.learncast.data.model.ActionType
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class LessonDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var lessonDao: LessonDao
    private lateinit var snipDao: SnipDao
    private lateinit var outboxDao: OutboxDao
    private lateinit var downloadDao: DownloadDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        lessonDao = database.getLessonDao()
        snipDao = database.getSnipDao()
        outboxDao = database.getOutboxDao()
        downloadDao = database.getDownloadDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    // ========== Insert Tests ==========

    @Test
    fun insertShouldAddLessonsToDatabase() = runTest {
        // Given
        val lessons = createLessons(2)

        // When
        lessonDao.insert(lessons)

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == lessons[0].id && it.title == lessons[0].title })
        assertTrue(result.any { it.id == lessons[1].id && it.title == lessons[1].title })
    }

    @Test
    fun insertWithOnConflictStrategyReplaceShouldUpdateExistingLesson() = runTest {
        // Given
        val originalLesson = createLesson(id = 1, title = "Original Title")
        lessonDao.insert(listOf(originalLesson))

        // When
        val updatedLesson = createLesson(id = 1, title = "Updated Title")
        lessonDao.insert(listOf(updatedLesson))

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
        assertEquals("Updated Title", result[0].title)
    }

    @Test
    fun insertEmptyListShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        lessonDao.insert(emptyList())
    }

    @Test
    fun insertWithLessonsAndStatesShouldAddBoth() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = lessons.map { createLessonStateInput(it.id) }

        // When
        lessonDao.insert(lessons, states)

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(2, result.size)
    }

    // ========== Delete Tests ==========

    @Test
    fun deleteShouldRemoveLessonsWithSpecifiedIds() = runTest {
        // Given
        val lessons = createLessons(count = 3)
        lessonDao.insert(lessons)

        // When
        lessonDao.delete(listOf(1L, 3L))

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun deleteWithEmptyListShouldNotFail() = runTest {
        // Given
        val lessons = createLessons(count = 1)
        lessonDao.insert(lessons)

        // When
        lessonDao.delete(emptyList())

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
    }

    @Test
    fun deleteWithNonExistentIdsShouldNotFail() = runTest {
        // Given
        val lessons = createLessons(count = 2)
        lessonDao.insert(lessons)

        // When
        lessonDao.delete(listOf(999L, 1000L))

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(2, result.size)
    }

    // ========== Upsert Tests ==========

    @Test
    fun upsertProgressShouldInsertNewState() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        lessonDao.insert(listOf(lesson))

        val progressInput = LessonProgressInput(
            lessonId = 1,
            startedAt = LocalDateTime(2024, 1, 1, 10, 0, 0),
            lastPositionMs = 30.seconds,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = null
        )

        // When
        lessonDao.upsertProgress(progressInput)

        // Then - Should not fail, state should be created
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
    }

    @Test
    fun upsertProgressShouldUpdateExistingState() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val initialState = createLessonStateInput(
            lessonId = 1,
            status = UserProgressStatus.NOT_STARTED,
            lastPositionMs = 0.seconds
        )
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(initialState))

        // When
        val updatedProgress = LessonProgressInput(
            lessonId = 1,
            startedAt = LocalDateTime(2024, 1, 1, 10, 0, 0),
            lastPositionMs = 120.seconds,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = null
        )
        lessonDao.upsertProgress(updatedProgress)

        // Then - State should be updated
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
    }

    @Test
    fun upsertStatesShouldInsertMultipleStates() = runTest {
        // Given
        val lessons = createLessons(3)
        lessonDao.insert(lessons)

        val states = lessons.map { createLessonStateInput(it.id) }

        // When
        lessonDao.upsertStates(states)

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(3, result.size)
    }

    @Test
    fun upsertStatesWithEmptyListShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        lessonDao.upsertStates(emptyList())
    }

    // ========== Update Tests ==========

    @Test
    fun updateUserSnipCountShouldModifyCount() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))

        // When
        lessonDao.updateUserSnipCount(lessonId = 1, count = 10)

        // Then - Verify update succeeded
        val result = lessonDao.getUserSnipCount(lesson.id)
        assertEquals(10, result)
    }

    @Test
    fun updateUserSnipCountWithNonExistentLessonShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        lessonDao.updateUserSnipCount(lessonId = 999, count = 10)
    }

    @Test
    fun updateListenCountShouldModifyCount() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = 1, listenCount = 2)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))

        // When
        lessonDao.updateListenCount(lessonId = 1, count = 5)

        // Then - Verify update succeeded
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
    }

    @Test
    fun updateListenCountWithNonExistentLessonShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        lessonDao.updateListenCount(lessonId = 999, count = 10)
    }

    // ========== Get Lessons by Topic and Author Tests ==========

    @Test
    fun getLessonsShouldReturnLessonsForTopicAndAuthor() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, topicId = 1, authorId = 1),
            createLesson(id = 2, topicId = 1, authorId = 1),
            createLesson(id = 3, topicId = 2, authorId = 1),
            createLesson(id = 4, topicId = 1, authorId = 2)
        )
        lessonDao.insert(lessons)

        // When
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.topicId == 1L && it.authorId == 1L })
    }

    @Test
    fun getLessonsShouldOrderByCreatedAtAsc() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, createdAt = LocalDateTime(2024, 1, 3, 0, 0, 0)),
            createLesson(id = 2, createdAt = LocalDateTime(2024, 1, 1, 0, 0, 0)),
            createLesson(id = 3, createdAt = LocalDateTime(2024, 1, 2, 0, 0, 0))
        )
        lessonDao.insert(lessons)

        // When
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].id) // Earliest date
        assertEquals(3L, result[1].id)
        assertEquals(1L, result[2].id) // Latest date
    }

    @Test
    fun getLessonsShouldReturnEmptyListWhenNoMatchFound() = runTest {
        // Given
        val lessons = createLessons(count = 2, topicId = 1, authorId = 1)
        lessonDao.insert(lessons)

        // When
        val result = lessonDao.getLessons(topicId = 999, authorId = 1)

        // Then
        assertEquals(0, result.size)
    }

    // ========== Get Lessons with Filters (PagingSource) Tests ==========

    @Test
    fun getLessonsWithNullFiltersShouldReturnAllLessons() = runTest {
        // Given
        val lessons = createLessons(3)
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun getLessonsWithBlankSearchShouldReturnAllLessons() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = "  ",
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getLessonsWithSearchShouldFilterByTitle() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, title = "Introduction to Kotlin"),
            createLesson(id = 2, title = "Advanced Kotlin"),
            createLesson(id = 3, title = "Java Basics")
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = "Kotlin",
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.lesson.title.contains("Kotlin", ignoreCase = true) })
    }

    @Test
    fun getLessonsWithSearchShouldBeCaseInsensitive() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, title = "KOTLIN BASICS"),
            createLesson(id = 2, title = "kotlin advanced")
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = "kotlin",
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getLessonsWithAuthorIdShouldFilterByAuthor() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, authorId = 1),
            createLesson(id = 2, authorId = 1),
            createLesson(id = 3, authorId = 2)
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = 1,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.lesson.authorId == 1L })
    }

    @Test
    fun getLessonsWithTopicIdShouldFilterByTopic() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, topicId = 10),
            createLesson(id = 2, topicId = 10),
            createLesson(id = 3, topicId = 20)
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = 10,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.lesson.topicId == 10L })
    }

    @Test
    fun getLessonsWithIsFavouriteShouldFilterByFavourite() = runTest {
        // Given
        val lessons = createLessons(3)
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = true),
            createLessonStateInput(lessonId = 2, isFavourite = false),
            createLessonStateInput(lessonId = 3, isFavourite = true)
        )
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = true,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.state.isFavourite })
    }

    @Test
    fun getLessonsWithStatusShouldFilterByProgressStatus() = runTest {
        // Given
        val lessons = createLessons(3)
        val states = listOf(
            createLessonStateInput(lessonId = 1, status = UserProgressStatus.NOT_STARTED),
            createLessonStateInput(lessonId = 2, status = UserProgressStatus.IN_PROGRESS),
            createLessonStateInput(lessonId = 3, status = UserProgressStatus.COMPLETED)
        )
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = UserProgressStatus.IN_PROGRESS,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals(UserProgressStatus.IN_PROGRESS, result[0].state.status)
    }

    @Test
    fun getLessonsWithMultipleFiltersShouldApplyAll() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, title = "Kotlin Basics", authorId = 1, topicId = 10),
            createLesson(id = 2, title = "Kotlin Advanced", authorId = 1, topicId = 10),
            createLesson(id = 3, title = "Java Basics", authorId = 1, topicId = 10),
            createLesson(id = 4, title = "Kotlin Flow", authorId = 2, topicId = 10)
        )
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = true, status = UserProgressStatus.IN_PROGRESS),
            createLessonStateInput(lessonId = 2, isFavourite = true, status = UserProgressStatus.NOT_STARTED),
            createLessonStateInput(lessonId = 3, isFavourite = false, status = UserProgressStatus.IN_PROGRESS),
            createLessonStateInput(lessonId = 4, isFavourite = true, status = UserProgressStatus.IN_PROGRESS)
        )
        lessonDao.insert(lessons, states)

        // When - Search for "Kotlin" + authorId=1 + topicId=10 + isFavourite=true + status=IN_PROGRESS
        val result = lessonDao.getLessons(
            search = "Kotlin",
            authorId = 1,
            topicId = 10,
            isFavourite = true,
            status = UserProgressStatus.IN_PROGRESS,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals(1L, result[0].lesson.id)
    }

    @Test
    fun getLessonsWithSortByCreatedAtAscShouldOrderCorrectly() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, createdAt = LocalDateTime(2024, 1, 3, 0, 0, 0)),
            createLesson(id = 2, createdAt = LocalDateTime(2024, 1, 1, 0, 0, 0)),
            createLesson(id = 3, createdAt = LocalDateTime(2024, 1, 2, 0, 0, 0))
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = QuerySort.CREATED_AT,
            order = QueryOrder.ASC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].lesson.id) // Earliest
        assertEquals(3L, result[1].lesson.id)
        assertEquals(1L, result[2].lesson.id) // Latest
    }

    @Test
    fun getLessonsWithSortByCreatedAtDescShouldOrderCorrectly() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, createdAt = LocalDateTime(2024, 1, 3, 0, 0, 0)),
            createLesson(id = 2, createdAt = LocalDateTime(2024, 1, 1, 0, 0, 0)),
            createLesson(id = 3, createdAt = LocalDateTime(2024, 1, 2, 0, 0, 0))
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = QuerySort.CREATED_AT,
            order = QueryOrder.DESC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(1L, result[0].lesson.id) // Latest
        assertEquals(3L, result[1].lesson.id)
        assertEquals(2L, result[2].lesson.id) // Earliest
    }

    @Test
    fun getLessonsWithSortBySnipCountAscShouldOrderCorrectly() = runTest {
        // Given
        val lessons = createLessons(3)
        val states = listOf(
            createLessonStateInput(lessonId = 1, snipCount = 10),
            createLessonStateInput(lessonId = 2, snipCount = 5),
            createLessonStateInput(lessonId = 3, snipCount = 15)
        )
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = QuerySort.SNIP_COUNT,
            order = QueryOrder.ASC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(2L, result[0].lesson.id) // snipCount = 5
        assertEquals(1L, result[1].lesson.id) // snipCount = 10
        assertEquals(3L, result[2].lesson.id) // snipCount = 15
    }

    @Test
    fun getLessonsWithSortBySnipCountDescShouldOrderCorrectly() = runTest {
        // Given
        val lessons = createLessons(3)
        val states = listOf(
            createLessonStateInput(lessonId = 1, snipCount = 10),
            createLessonStateInput(lessonId = 2, snipCount = 5),
            createLessonStateInput(lessonId = 3, snipCount = 15)
        )
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = QuerySort.SNIP_COUNT,
            order = QueryOrder.DESC
        ).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(3L, result[0].lesson.id) // snipCount = 15
        assertEquals(1L, result[1].lesson.id) // snipCount = 10
        assertEquals(2L, result[2].lesson.id) // snipCount = 5
    }

    @Test
    fun getLessonsShouldReturnEmptyListWhenNoMatchesFound() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = "NonexistentTitle",
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getLessonsShouldHandleSpecialCharactersInSearch() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, title = "C++ Programming"),
            createLesson(id = 2, title = "What's New in Kotlin?")
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = "C++",
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("C++ Programming", result[0].lesson.title)
    }

    @Test
    fun getLessonsShouldHandleNullableTopicId() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, topicId = null),
            createLesson(id = 2, topicId = 10)
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
    }

    // ========== Get Downloaded Lessons Tests ==========

    @Test
    fun getDownloadedLessonsWithNullSearchShouldReturnAllDownloadedLessons() = runTest {
        // Given - This test would require download state setup
        // When
        val result = lessonDao.getDownloadedLessons(
            search = null,
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(0, result.size) // No downloads yet
    }

    @Test
    fun getDownloadedLessonsWithBlankSearchShouldReturnAllDownloadedLessons() = runTest {
        // When
        val result = lessonDao.getDownloadedLessons(
            search = "  ",
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getDownloadedLessonsWithSearchShouldFilterByTitle() = runTest {
        // When
        val result = lessonDao.getDownloadedLessons(
            search = "Kotlin",
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(0, result.size) // No downloads matching search
    }

    // ========== Edge Cases and Validation ==========

    @Test
    fun insertShouldHandleLargeDataset() = runTest {
        // Given
        val lessons = createLessons(count = 100)

        // When
        lessonDao.insert(lessons)

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(100, result.size)
    }

    @Test
    fun getLessonsShouldHandleNullableFields() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, description = null, coverImagePath = null, topicId = null, topicTitle = null)
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertNotNull(result[0].lesson)
    }

    @Test
    fun upsertStatesShouldHandleNullableStateFields() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        lessonDao.insert(listOf(lesson))

        val state = LessonStateInput(
            lessonId = 1,
            listenCount = 0,
            snipCount = 0,
            isFavourite = false,
            startedAt = null,
            lastPositionMs = null,
            status = UserProgressStatus.NOT_STARTED,
            completedAt = null
        )

        // When
        lessonDao.upsertStates(listOf(state))

        // Then
        val result = lessonDao.getLessons(topicId = 1, authorId = 1)
        assertEquals(1, result.size)
    }

    // ========== Integration Tests: getUserSnipCount ==========

    @Test
    fun getUserSnipCountShouldReturnStateCountWhenNoSnips() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 5)


        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        assertEquals(5, count)
    }

    @Test
    fun getUserSnipCountShouldAddCreateActionsFromOutbox() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 5)

        // Insert SNIPs for this lesson
        val snips = listOf(
            createSnip(id = 1, lessonId = 1, clientSnipId = "snip-uuid-1"),
            createSnip(id = 2, lessonId = 1, clientSnipId = "snip-uuid-2")
        )
        snipDao.insert(snips)

        // Insert CREATE actions in OUTBOX for these snips
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntries = listOf(
            createOutboxEntity(
                id = 1,
                referenceId = 1,
                referenceUuid = "snip-uuid-1",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.CREATE,
                createdAt = now
            ),
            createOutboxEntity(
                id = 2,
                referenceId = 2,
                referenceUuid = "snip-uuid-2",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.CREATE,
                createdAt = now
            )
        )
        outboxEntries.forEach { outboxDao.insert(it) }

        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        // Initial count (5) + 2 CREATE actions = 7
        assertEquals(7, count)
    }

    @Test
    fun getUserSnipCountShouldSubtractDeleteActionsFromOutbox() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 10)


        // Insert SNIPs for this lesson
        val snips = listOf(
            createSnip(id = 1, lessonId = 1, clientSnipId = "snip-uuid-1"),
            createSnip(id = 2, lessonId = 1, clientSnipId = "snip-uuid-2")
        )
        snipDao.insert(snips)

        // Insert DELETE actions in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntries = listOf(
            createOutboxEntity(
                id = 1,
                referenceId = 1,
                referenceUuid = "snip-uuid-1",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.DELETE,
                createdAt = now
            ),
            createOutboxEntity(
                id = 2,
                referenceId = 2,
                referenceUuid = "snip-uuid-2",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.DELETE,
                createdAt = now
            )
        )
        outboxEntries.forEach { outboxDao.insert(it) }

        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        // Initial count (10) - 2 DELETE actions = 8
        assertEquals(8, count)
    }

    @Test
    fun getUserSnipCountShouldHandleMixOfCreateAndDeleteActions() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 10)

        // Insert SNIPs for this lesson
        val snips = listOf(
            createSnip(id = 1, lessonId = 1, clientSnipId = "snip-uuid-1"),
            createSnip(id = 2, lessonId = 1, clientSnipId = "snip-uuid-2"),
            createSnip(id = 3, lessonId = 1, clientSnipId = "snip-uuid-3"),
            createSnip(id = 4, lessonId = 1, clientSnipId = "snip-uuid-4")
        )
        snipDao.insert(snips)

        // Mix of CREATE and DELETE actions
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntries = listOf(
            createOutboxEntity(
                id = 1,
                referenceId = 1,
                referenceUuid = "snip-uuid-1",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.CREATE,
                createdAt = now
            ),
            createOutboxEntity(
                id = 2,
                referenceId = 2,
                referenceUuid = "snip-uuid-2",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.CREATE,
                createdAt = now
            ),
            createOutboxEntity(
                id = 3,
                referenceId = 3,
                referenceUuid = "snip-uuid-3",
                referenceType = ReferenceType.SNIP,
                actionType = ActionType.DELETE,
                createdAt = now
            )
        )
        outboxEntries.forEach { outboxDao.insert(it) }

        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        // Initial count (10) + 2 CREATE - 1 DELETE = 11
        assertEquals(11, count)
    }

    @Test
    fun getUserSnipCountShouldIgnoreNonSnipOutboxEntries() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 5)

        // Insert outbox entries with different referenceType (should be ignored)
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntries = listOf(
            createOutboxEntity(
                id = 1,
                referenceId = 1,
                referenceUuid = "lesson-uuid",
                referenceType = ReferenceType.LESSON,
                actionType = ActionType.CREATE,
                createdAt = now
            ),
            createOutboxEntity(
                id = 2,
                referenceId = 1,
                referenceUuid = "lesson-uuid-2",
                referenceType = ReferenceType.LESSON,
                actionType = ActionType.DELETE,
                createdAt = now
            )
        )
        outboxEntries.forEach { outboxDao.insert(it) }

        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        // Should remain 5 (outbox entries for LESSON type are ignored)
        assertEquals(5, count)
    }

    @Test
    fun getUserSnipCountShouldHandleSnipsWithoutOutboxEntries() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(lessonId = lesson.id)
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))
        lessonDao.updateUserSnipCount(lesson.id, 5)

        // Insert SNIPs but no corresponding OUTBOX entries
        val snips = listOf(
            createSnip(id = 1, lessonId = 1, clientSnipId = "snip-uuid-1"),
            createSnip(id = 2, lessonId = 1, clientSnipId = "snip-uuid-2")
        )
        snipDao.insert(snips)

        // When
        val count = lessonDao.getUserSnipCount(lessonId = 1)

        // Then
        // Should remain 5 (no outbox actions to add/subtract)
        assertEquals(5, count)
    }

    // ========== Integration Tests: getLessons with OUTBOX ==========

    @Test
    fun getLessonsShouldShowFavouriteWhenOutboxHasFavouriteAction() = runTest {
        // Given
        val lessons = createLessons(1)
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = false)
        )
        lessonDao.insert(lessons, states)

        // Insert FAVOURITE action in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.FAVOURITE,
            createdAt = now
        )
        outboxDao.insert(outboxEntry)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].state.isFavourite)
    }

    @Test
    fun getLessonsShouldShowNotFavouriteWhenOutboxHasRemoveFavouriteAction() = runTest {
        // Given
        val lessons = createLessons(1)
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = true)
        )
        lessonDao.insert(lessons, states)

        // Insert REMOVE_FAVOURITE action in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.REMOVE_FAVOURITE,
            createdAt = now
        )
        outboxDao.insert(outboxEntry)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertTrue(!result[0].state.isFavourite)
    }

    @Test
    fun getLessonsShouldUseStateWhenNoOutboxAction() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = true),
            createLessonStateInput(lessonId = 2, isFavourite = false)
        )
        lessonDao.insert(lessons, states)

        // No OUTBOX entries

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result[0].state.isFavourite)
        assertTrue(!result[1].state.isFavourite)
    }

    @Test
    fun getLessonsShouldCoalesceProgressFromLessonOutbox() = runTest {
        // Given
        val lesson = createLesson(id = 1)
        val state = createLessonStateInput(
            lessonId = 1,
            startedAt = LocalDateTime(2024, 1, 1, 10, 0, 0),
            lastPositionMs = 30.seconds,
            status = UserProgressStatus.IN_PROGRESS,
            completedAt = null
        )
        lessonDao.insert(listOf(lesson))
        lessonDao.upsertStates(listOf(state))

        // Insert LESSON_OUTBOX with newer values
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.UPDATE,
            createdAt = now
        )
        val outboxId = outboxDao.insert(outboxEntry)

        val lessonOutbox = LessonOutboxEntity(
            id = 0,
            outboxId = outboxId,
            lessonId = 1,
            startedAt = LocalDateTime(2024, 1, 2, 10, 0, 0),
            lastPositionMs = 120.seconds,
            status = UserProgressStatus.COMPLETED,
            completedAt = LocalDateTime(2024, 1, 2, 11, 0, 0)
        )
        outboxDao.insert(lessonOutbox)

        // When
        val result = lessonDao.getLessons(
            search = null,
            authorId = null,
            topicId = null,
            isFavourite = null,
            status = null,
            isDownloaded = null,
            sort = null,
            order = null
        ).loadList()

        // Then - Should use LESSON_OUTBOX values via COALESCE
        assertEquals(1, result.size)
        assertEquals(120.seconds, result[0].state.lastPositionMs)
        assertEquals(UserProgressStatus.COMPLETED, result[0].state.status)
    }

    // ========== Integration Tests: getDownloadedLessons ==========

    @Test
    fun getDownloadedLessonsShouldReturnLessonsWithDownloadState() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // Insert download states
        val downloadStates = listOf(
            createDownloadState(
                id = 0,
                referenceId = 1,
                referenceUuid = "lesson-uuid-1",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.COMPLETED,
                percentDownloaded = 100f
            ),
            createDownloadState(
                id = 0,
                referenceId = 2,
                referenceUuid = "lesson-uuid-2",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.DOWNLOADING,
                percentDownloaded = 50f
            )
        )
        downloadStates.forEach { downloadDao.insert(it) }

        // When
        val result = lessonDao.getDownloadedLessons(
            search = null,
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(2, result.size)
        assertNotNull(result[0].downloadState)
        assertNotNull(result[1].downloadState)
    }

    @Test
    fun getDownloadedLessonsShouldExcludeRemovingState() = runTest {
        // Given
        val lessons = createLessons(2)
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // Insert download states - one REMOVING
        val downloadStates = listOf(
            createDownloadState(
                id = 0,
                referenceId = 1,
                referenceUuid = "lesson-uuid-1",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.COMPLETED,
                percentDownloaded = 100f
            ),
            createDownloadState(
                id = 0,
                referenceId = 2,
                referenceUuid = "lesson-uuid-2",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.REMOVING,
                percentDownloaded = 0f
            )
        )
        downloadStates.forEach { downloadDao.insert(it) }

        // When
        val result = lessonDao.getDownloadedLessons(
            search = null,
            isDownloaded = null
        ).loadList()

        // Then - Should exclude REMOVING state
        assertEquals(1, result.size)
        assertEquals(DownloadState.COMPLETED, result[0].downloadState)
    }

    @Test
    fun getDownloadedLessonsShouldFilterBySearch() = runTest {
        // Given
        val lessons = listOf(
            createLesson(id = 1, title = "Kotlin Basics"),
            createLesson(id = 2, title = "Java Advanced")
        )
        val states = lessons.map { createLessonStateInput(it.id) }
        lessonDao.insert(lessons, states)

        // Insert download states
        val downloadStates = listOf(
            createDownloadState(
                id = 0,
                referenceId = 1,
                referenceUuid = "lesson-uuid-1",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.COMPLETED
            ),
            createDownloadState(
                id = 0,
                referenceId = 2,
                referenceUuid = "lesson-uuid-2",
                referenceType = ReferenceType.LESSON,
                state = DownloadState.COMPLETED
            )
        )
        downloadStates.forEach { downloadDao.insert(it) }

        // When
        val result = lessonDao.getDownloadedLessons(
            search = "Kotlin",
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("Kotlin Basics", result[0].lesson.title)
    }

    @Test
    fun getDownloadedLessonsShouldIncludeFavouriteFromOutbox() = runTest {
        // Given
        val lessons = createLessons(1)
        val states = listOf(
            createLessonStateInput(lessonId = 1, isFavourite = false)
        )
        lessonDao.insert(lessons, states)

        // Insert download state
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            state = DownloadState.COMPLETED
        )
        downloadDao.insert(downloadState)

        // Insert FAVOURITE action in OUTBOX
        val now = LocalDateTime(2024, 1, 1, 0, 0, 0)
        val outboxEntry = createOutboxEntity(
            id = 1,
            referenceId = 1,
            referenceUuid = "lesson-uuid-1",
            referenceType = ReferenceType.LESSON,
            actionType = ActionType.FAVOURITE,
            createdAt = now
        )
        outboxDao.insert(outboxEntry)

        // When
        val result = lessonDao.getDownloadedLessons(
            search = null,
            isDownloaded = null
        ).loadList()

        // Then
        assertEquals(1, result.size)
        assertTrue(result[0].state.isFavourite)
    }


}
