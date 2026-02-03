package me.anasmusa.learncast.data.local.db.topic

import kotlinx.coroutines.test.runTest
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Topic.createTopic
import me.anasmusa.learncast.data.local.db.TestFixtures.Topic.createTopics
import me.anasmusa.learncast.data.local.db.TestFixtures.Topic.createTopicsWithTitles
import me.anasmusa.learncast.data.local.db.TestFixtures.loadList
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopicDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var topicDao: TopicDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        topicDao = database.getTopicDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun insertShouldAddTopicsToDatabase() = runTest {
        // Given
        val topics = createTopics(2)

        // When
        topicDao.insert(topics)

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == topics[0].id && it.title == topics[0].title })
        assertTrue(result.any { it.id == topics[1].id && it.title == topics[1].title })
    }

    @Test
    fun insertWithOnConflictStrategyReplaceShouldUpdateExistingTopic() = runTest {
        // Given
        val originalTopic = createTopic(id = 1, title = "Original Title", lessonCount = 5)
        topicDao.insert(listOf(originalTopic))

        // When
        val updatedTopic = createTopic(id = 1, title = "Updated Title", lessonCount = 10)
        topicDao.insert(listOf(updatedTopic))

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(1, result.size)
        assertEquals("Updated Title", result[0].title)
        assertEquals(10L, result[0].lessonCount)
    }

    @Test
    fun deleteShouldRemoveTopicsWithSpecifiedIds() = runTest {
        // Given
        val topics = createTopics(count = 3)
        topicDao.insert(topics)

        // When
        topicDao.delete(listOf(1L, 3L))

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun deleteWithEmptyListShouldNotAffectDatabase() = runTest {
        // Given
        val topics = createTopics(count = 1)
        topicDao.insert(topics)

        // When
        topicDao.delete(emptyList())

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(1, result.size)
    }

    @Test
    fun getTopicsWithNullSearchAndNullAuthorIdShouldReturnAllTopics() = runTest {
        // Given
        val topics = createTopics(count = 3)
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = null).loadList()

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun getTopicsWithBlankSearchAndNullAuthorIdShouldReturnAllTopics() = runTest {
        // Given
        val topics = createTopics(count = 2)
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "  ", authorId = null).loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getTopicsWithSearchTermShouldFilterByTitle() = runTest {
        // Given
        val topics = createTopicsWithTitles(
            "Introduction to Kotlin",
            "Advanced Kotlin Coroutines",
            "Java Design Patterns",
            "Kotlin Flow Basics"
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "Kotlin", authorId = null).loadList()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.title.contains("Kotlin", ignoreCase = true) })
    }

    @Test
    fun getTopicsWithSearchTermShouldBeCaseInsensitive() = runTest {
        // Given
        val topics = createTopicsWithTitles(
            "Introduction to Kotlin",
            "ADVANCED KOTLIN COROUTINES"
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "kotlin", authorId = null).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.title == "Introduction to Kotlin" })
        assertTrue(result.any { it.title == "ADVANCED KOTLIN COROUTINES" })
    }

    @Test
    fun getTopicsWithAuthorIdShouldFilterByAuthor() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Topic 1", authorId = 1),
            createTopic(id = 2, title = "Topic 2", authorId = 1),
            createTopic(id = 3, title = "Topic 3", authorId = 2),
            createTopic(id = 4, title = "Topic 4", authorId = 2),
            createTopic(id = 5, title = "Topic 5", authorId = 3)
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = 1).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.authorId == 1L })
    }

    @Test
    fun getTopicsWithSearchAndAuthorIdShouldFilterByBoth() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Kotlin Basics", authorId = 1),
            createTopic(id = 2, title = "Kotlin Advanced", authorId = 1),
            createTopic(id = 3, title = "Java Basics", authorId = 1),
            createTopic(id = 4, title = "Kotlin Flow", authorId = 2),
            createTopic(id = 5, title = "Python Basics", authorId = 2)
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "Kotlin", authorId = 1).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.authorId == 1L })
        assertTrue(result.all { it.title.contains("Kotlin", ignoreCase = true) })
    }

    @Test
    fun getTopicsShouldOrderByIdDescending() = runTest {
        // Given
        val topics = createTopics(5)
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = null).loadList()

        // Then
        assertEquals(5, result.size)
        assertEquals(5L, result[0].id)
        assertEquals(4L, result[1].id)
        assertEquals(3L, result[2].id)
        assertEquals(2L, result[3].id)
        assertEquals(1L, result[4].id)
    }

    @Test
    fun getTopicsWithAuthorIdAndBlankSearchShouldReturnAllTopicsForAuthor() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Topic 1", authorId = 1),
            createTopic(id = 2, title = "Topic 2", authorId = 1),
            createTopic(id = 3, title = "Topic 3", authorId = 2)
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "  ", authorId = 1).loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.all { it.authorId == 1L })
    }

    @Test
    fun getTopicsShouldReturnEmptyListWhenNoMatchesFound() = runTest {
        // Given
        val topics = createTopics(count = 1)
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "NonexistentTopic", authorId = null).loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun getTopicsShouldReturnEmptyListWhenAuthorHasNoTopics() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Topic 1", authorId = 1)
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = 999).loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun insertEmptyListShouldNotFail() = runTest {
        // When
        topicDao.insert(emptyList())

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(0, result.size)
    }

    @Test
    fun getTopicsShouldHandleSpecialCharactersInSearch() = runTest {
        // Given
        val topics = createTopicsWithTitles(
            "C++ Programming",
            "Object-Oriented Design",
            "What's New in Kotlin?"
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "C++", authorId = null).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("C++ Programming", result[0].title)
    }

    @Test
    fun getTopicsShouldHandlePartialMatches() = runTest {
        // Given
        val topics = createTopicsWithTitles(
            "Introduction to Programming",
            "Advanced Programming Techniques",
            "Programming Best Practices"
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = "gram", authorId = null).loadList()

        // Then
        assertEquals(3, result.size)
        assertTrue(result.all { it.title.contains("gram", ignoreCase = true) })
    }

    @Test
    fun insertShouldHandleLargeDatasetEfficiently() = runTest {
        // Given
        val topics = createTopics(count = 100)

        // When
        topicDao.insert(topics)

        // Then
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(100, result.size)
    }

    @Test
    fun getTopicsShouldHandleMultipleAuthorsWithSameContent() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Kotlin Basics", authorId = 1, authorName = "John Doe"),
            createTopic(id = 2, title = "Kotlin Basics", authorId = 2, authorName = "Jane Smith"),
            createTopic(id = 3, title = "Kotlin Basics", authorId = 3, authorName = "Bob Johnson")
        )
        topicDao.insert(topics)

        // When - Filter by authorId
        val result = topicDao.getTopics(search = null, authorId = 2).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals(2L, result[0].authorId)
        assertEquals("Jane Smith", result[0].authorName)
    }

    @Test
    fun getTopicsShouldHandleTopicsWithNullableFields() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Topic 1", description = null, coverImagePath = null),
            createTopic(id = 2, title = "Topic 2", description = "Description", coverImagePath = "/path/to/image.jpg")
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = null).loadList()

        // Then
        assertEquals(2, result.size)
        assertEquals(null, result[1].description)
        assertEquals(null, result[1].coverImagePath)
    }

    @Test
    fun getTopicsShouldFilterCorrectlyWithSearchAndMultipleAuthors() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Kotlin for Beginners", authorId = 1),
            createTopic(id = 2, title = "Kotlin Advanced", authorId = 1),
            createTopic(id = 3, title = "Kotlin Coroutines", authorId = 2),
            createTopic(id = 4, title = "Java Basics", authorId = 2),
            createTopic(id = 5, title = "Kotlin Flow", authorId = 3)
        )
        topicDao.insert(topics)

        // When - Search for Kotlin with author 2
        val result = topicDao.getTopics(search = "Kotlin", authorId = 2).loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("Kotlin Coroutines", result[0].title)
        assertEquals(2L, result[0].authorId)
    }

    @Test
    fun getTopicsShouldHandleEmptyStringSearchDifferentlyThanNull() = runTest {
        // Given
        val topics = createTopics(count = 3)
        topicDao.insert(topics)

        // When - Empty string (should be treated as blank and return all)
        val result = topicDao.getTopics(search = "", authorId = null).loadList()

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun getTopicsShouldHandleTopicsWithDifferentCompletionStates() = runTest {
        // Given
        val topics = listOf(
            createTopic(id = 1, title = "Topic 1", lessonCount = 10, completedLessonCount = 0),
            createTopic(id = 2, title = "Topic 2", lessonCount = 10, completedLessonCount = 5),
            createTopic(id = 3, title = "Topic 3", lessonCount = 10, completedLessonCount = 10)
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = null).loadList()

        // Then
        assertEquals(3, result.size)
        assertEquals(0L, result[2].completedLessonCount)
        assertEquals(5L, result[1].completedLessonCount)
        assertEquals(10L, result[0].completedLessonCount)
    }

    @Test
    fun getTopicsShouldHandleUnicodeCharactersInTitles() = runTest {
        // Given
        val topics = createTopicsWithTitles(
            "Introducción a la Programación",
            "Programmierung für Anfänger",
            "プログラミング入門",
            "编程基础"
        )
        topicDao.insert(topics)

        // When
        val result = topicDao.getTopics(search = null, authorId = null).loadList()

        // Then
        assertEquals(4, result.size)
        assertTrue(result.any { it.title == "Introducción a la Programación" })
        assertTrue(result.any { it.title == "Programmierung für Anfänger" })
    }

    @Test
    fun deleteShouldHandleNonExistentIds() = runTest {
        // Given
        val topics = createTopics(count = 2)
        topicDao.insert(topics)

        // When - Try to delete non-existent IDs
        topicDao.delete(listOf(999L, 1000L))

        // Then - Original topics should remain
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(2, result.size)
    }

    @Test
    fun deleteShouldHandleMixOfExistingAndNonExistentIds() = runTest {
        // Given
        val topics = createTopics(count = 3)
        topicDao.insert(topics)

        // When - Delete mix of existing and non-existent IDs
        topicDao.delete(listOf(1L, 999L, 3L))

        // Then - Only topic 2 should remain
        val result = topicDao.getTopics(search = null, authorId = null).loadList()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }
}
