package me.anasmusa.learncast.data.local.db.pagingstate

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.PagingState.createPagingState
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PagingStateDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var pagingStateDao: PagingStateDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        pagingStateDao = database.getPagingStateDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    // ========== Upsert Tests ==========

    @Test
    fun upsertShouldInsertNewItem() = runTest {
        // Given
        val item = createPagingState(
            resourceType = "lessons",
            queryKey = "search=kotlin"
        )

        // When
        pagingStateDao.upsert(item)

        // Then
        val result = pagingStateDao.get(
            resourceType = "lessons",
            queryKey = "search=kotlin"
        )
        assertNotNull(result)
        assertEquals("lessons", result.resourceType)
        assertEquals("search=kotlin", result.queryKey)
    }

    @Test
    fun upsertShouldUpdateExistingItemWithSameKeys() = runTest {
        // Given
        val originalItem = createPagingState(
            resourceType = "topics",
            queryKey = "authorId=1",
            lastDeletionSync = LocalDateTime(2024, 1, 1, 10, 0, 0)
        )
        pagingStateDao.upsert(originalItem)

        // When - Upsert with same keys but different timestamp
        val updatedItem = createPagingState(
            resourceType = "topics",
            queryKey = "authorId=1",
            lastDeletionSync = LocalDateTime(2024, 1, 2, 10, 0, 0)
        )
        pagingStateDao.upsert(updatedItem)

        // Then - Should update, not create duplicate
        val result = pagingStateDao.get(
            resourceType = "topics",
            queryKey = "authorId=1"
        )
        assertNotNull(result)
        assertEquals(LocalDateTime(2024, 1, 2, 10, 0, 0), result.lastDeletionSync)
    }

    @Test
    fun upsertShouldInsertDifferentItemsWithDifferentKeys() = runTest {
        // Given
        val item1 = createPagingState(
            resourceType = "lessons",
            queryKey = "topicId=1"
        )
        val item2 = createPagingState(
            resourceType = "lessons",
            queryKey = "topicId=2"
        )
        val item3 = createPagingState(
            resourceType = "topics",
            queryKey = "search=kotlin"
        )

        // When
        pagingStateDao.upsert(item1)
        pagingStateDao.upsert(item2)
        pagingStateDao.upsert(item3)

        // Then - All should exist independently
        assertNotNull(pagingStateDao.get("lessons", "topicId=1"))
        assertNotNull(pagingStateDao.get("lessons", "topicId=2"))
        assertNotNull(pagingStateDao.get("topics", "search=kotlin"))
    }

    // ========== Get Tests ==========

    @Test
    fun getShouldReturnItemWithMatchingCompositeKey() = runTest {
        // Given
        val item = createPagingState(
            resourceType = "snips",
            queryKey = "lessonId=100",
            lastDeletionSync = LocalDateTime(2024, 1, 15, 12, 30, 0)
        )
        pagingStateDao.upsert(item)

        // When
        val result = pagingStateDao.get(
            resourceType = "snips",
            queryKey = "lessonId=100"
        )

        // Then
        assertNotNull(result)
        assertEquals("snips", result.resourceType)
        assertEquals("lessonId=100", result.queryKey)
        assertEquals(LocalDateTime(2024, 1, 15, 12, 30, 0), result.lastDeletionSync)
    }

    @Test
    fun getShouldReturnNullWhenNoMatch() = runTest {
        // When
        val result = pagingStateDao.get(
            resourceType = "nonexistent",
            queryKey = "invalid"
        )

        // Then
        assertNull(result)
    }

    @Test
    fun getShouldRequireBothKeysToMatch() = runTest {
        // Given
        val item = createPagingState(
            resourceType = "lessons",
            queryKey = "search=kotlin"
        )
        pagingStateDao.upsert(item)

        // When - Query with different resourceType but same queryKey
        val result1 = pagingStateDao.get(
            resourceType = "topics",  // Different
            queryKey = "search=kotlin"
        )

        // When - Query with same resourceType but different queryKey
        val result2 = pagingStateDao.get(
            resourceType = "lessons",
            queryKey = "search=java"  // Different
        )

        // Then - Both should return null
        assertNull(result1)
        assertNull(result2)
    }

    @Test
    fun getShouldDistinguishBetweenSimilarKeys() = runTest {
        // Given
        val item1 = createPagingState(
            resourceType = "lessons",
            queryKey = "search=kotlin",
            lastDeletionSync = LocalDateTime(2024, 1, 1, 0, 0, 0)
        )
        val item2 = createPagingState(
            resourceType = "lessons",
            queryKey = "search=kotlin&authorId=1",  // Similar but different
            lastDeletionSync = LocalDateTime(2024, 1, 2, 0, 0, 0)
        )
        pagingStateDao.upsert(item1)
        pagingStateDao.upsert(item2)

        // When
        val result1 = pagingStateDao.get("lessons", "search=kotlin")
        val result2 = pagingStateDao.get("lessons", "search=kotlin&authorId=1")

        // Then - Should return different items
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(LocalDateTime(2024, 1, 1, 0, 0, 0), result1.lastDeletionSync)
        assertEquals(LocalDateTime(2024, 1, 2, 0, 0, 0), result2.lastDeletionSync)
    }

    @Test
    fun upsertMultipleTimesShouldKeepLatestValue() = runTest {
        // Given
        val resourceType = "downloads"
        val queryKey = "status=completed"

        // When - Upsert same key multiple times
        pagingStateDao.upsert(createPagingState(
            resourceType = resourceType,
            queryKey = queryKey,
            lastDeletionSync = LocalDateTime(2024, 1, 1, 0, 0, 0)
        ))
        pagingStateDao.upsert(createPagingState(
            resourceType = resourceType,
            queryKey = queryKey,
            lastDeletionSync = LocalDateTime(2024, 1, 2, 0, 0, 0)
        ))
        pagingStateDao.upsert(createPagingState(
            resourceType = resourceType,
            queryKey = queryKey,
            lastDeletionSync = LocalDateTime(2024, 1, 3, 0, 0, 0)
        ))

        // Then - Should have latest value
        val result = pagingStateDao.get(resourceType, queryKey)
        assertNotNull(result)
        assertEquals(LocalDateTime(2024, 1, 3, 0, 0, 0), result.lastDeletionSync)
    }

    // ========== Edge Cases ==========

    @Test
    fun shouldHandleEmptyStringsInKeys() = runTest {
        // Given
        val item = createPagingState(
            resourceType = "",
            queryKey = ""
        )

        // When
        pagingStateDao.upsert(item)

        // Then
        val result = pagingStateDao.get("", "")
        assertNotNull(result)
    }

    @Test
    fun shouldHandleSpecialCharactersInKeys() = runTest {
        // Given
        val item = createPagingState(
            resourceType = "lessons",
            queryKey = "search=C++&authorId=1&topicId=2"
        )

        // When
        pagingStateDao.upsert(item)

        // Then
        val result = pagingStateDao.get("lessons", "search=C++&authorId=1&topicId=2")
        assertNotNull(result)
        assertEquals("search=C++&authorId=1&topicId=2", result.queryKey)
    }

    @Test
    fun shouldHandleLongQueryKeys() = runTest {
        // Given
        val longQueryKey = "search=kotlin&authorId=1&topicId=2&lessonId=3&status=completed&sort=createdAt&order=desc&page=1&limit=20"
        val item = createPagingState(
            resourceType = "lessons",
            queryKey = longQueryKey
        )

        // When
        pagingStateDao.upsert(item)

        // Then
        val result = pagingStateDao.get("lessons", longQueryKey)
        assertNotNull(result)
        assertEquals(longQueryKey, result.queryKey)
    }

}
