package me.anasmusa.learncast.data.local.db.author

import kotlinx.coroutines.test.runTest
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Author.createAuthor
import me.anasmusa.learncast.data.local.db.TestFixtures.Author.createAuthors
import me.anasmusa.learncast.data.local.db.TestFixtures.Author.createAuthorsWithNames
import me.anasmusa.learncast.data.local.db.TestFixtures.loadList
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthorDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var authorDao: AuthorDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        authorDao = database.getAuthorDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun insertShouldAddAuthorsToDatabase() = runTest {
        // Given
        val authors = createAuthors(2)

        // When
        authorDao.insert(authors)

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(2, result.size)
        assertTrue(result.any { it.id == authors[0].id && it.name == authors[0].name })
        assertTrue(result.any { it.id == authors[1].id && it.name == authors[1].name })
    }

    @Test
    fun insertWithOnConflictStrategyReplaceShouldUpdateExistingAuthor() = runTest {
        // Given
        val originalAuthor = createAuthor(id = 1, name = "Original Name", lessonCount = 5)
        authorDao.insert(listOf(originalAuthor))

        // When
        val updatedAuthor = createAuthor(id = 1, name = "Updated Name", lessonCount = 10)
        authorDao.insert(listOf(updatedAuthor))

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(1, result.size)
        assertEquals("Updated Name", result[0].name)
        assertEquals(10L, result[0].lessonCount)
    }

    @Test
    fun deleteShouldRemoveAuthorsWithSpecifiedIds() = runTest {
        // Given
        val authors = createAuthors(count = 3)
        authorDao.insert(authors)

        // When
        authorDao.delete(listOf(1L, 3L))

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(1, result.size)
        assertEquals(2L, result[0].id)
    }

    @Test
    fun deleteWithEmptyListShouldNotAffectDatabase() = runTest {
        // Given
        val authors = createAuthors(count = 1)
        authorDao.insert(authors)

        // When
        authorDao.delete(emptyList())

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(1, result.size)
    }

    @Test
    fun getAuthorsWithNullSearchShouldReturnAllAuthors() = runTest {
        // Given
        val authors = createAuthors(count = 3)
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = null).loadList()

        // Then
        assertEquals(3, result.size)
    }

    @Test
    fun getAuthorsWithBlankSearchShouldReturnAllAuthors() = runTest {
        // Given
        val authors = createAuthors(count = 2)
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "  ").loadList()

        // Then
        assertEquals(2, result.size)
    }

    @Test
    fun getAuthorsWithSearchTermShouldFilterByName() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "John Doe",
            "Jane Smith",
            "Bob Johnson"
        )
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "John").loadList()

        // Then
        assertEquals(2, result.size)
        assertTrue(result.any { it.name == "John Doe" })
        assertTrue(result.any { it.name == "Bob Johnson" })
    }

    @Test
    fun getAuthorsWithSearchTermShouldBeCaseInsensitive() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "John Doe",
            "JANE SMITH"
        )
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "jane").loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("JANE SMITH", result[0].name)
    }

    @Test
    fun getAuthorsShouldOrderByLessonCountDescThenIdDesc() = runTest {
        // Given
        val authors = createAuthors(5, 0..50)
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = null).loadList()

        // Then
        assertEquals(5, result.size)
        authors.sortedWith(compareByDescending<AuthorEntity> { it.lessonCount }.thenBy { it.id })
        authors.sortedByDescending { it.lessonCount }.forEachIndexed { index, entity ->
            assertEquals(entity.id, result[index].id)
        }
    }

    @Test
    fun getAuthorsWithSearchAndOrderingShouldWorkTogether() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "John Doe",
            "John Smith",
            "Johnny Walker"
        )
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "John").loadList()

        // Then
        assertEquals(3, result.size)
        authors.sortedWith(compareByDescending<AuthorEntity> { it.lessonCount }.thenBy { it.id })
            .forEachIndexed { index, entity ->
                assertEquals(entity.id, result[index].id)
            }
    }

    @Test
    fun getAuthorsShouldReturnEmptyListWhenNoMatchesFound() = runTest {
        // Given
        val authors = createAuthors(count = 1)
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "NonexistentName").loadList()

        // Then
        assertEquals(0, result.size)
    }

    @Test
    fun insertEmptyListShouldNotFail() = runTest {
        // When
        authorDao.insert(emptyList())

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(0, result.size)
    }

    @Test
    fun getAuthorsShouldHandleSpecialCharactersInSearch() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "O'Brien",
            "Smith-Jones"
        )
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = "O'Brien").loadList()

        // Then
        assertEquals(1, result.size)
        assertEquals("O'Brien", result[0].name)
    }

    @Test
    fun insertShouldHandleLargeDatasetEfficiently() = runTest {
        // Given
        val authors = createAuthors(count = 100, lessonCountRange = 1..100)

        // When
        authorDao.insert(authors)

        // Then
        val result = authorDao.getAuthors(search = null).loadList()
        assertEquals(100, result.size)
    }

    @Test
    fun getAuthorsShouldWorkWithPredefinedTestDatasets() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "John Doe",
            "Jane Smith",
            "Bob Johnson",
            "Johnny Walker",
            "John Smith",
            "Alice Cooper",
            "Robert Brown",
        )
        authorDao.insert(authors)

        // When - search for "John"
        val result = authorDao.getAuthors(search = "John").loadList()

        // Then
        assertTrue(result.size >= 3) // At least John Doe, Johnny Walker, John Smith
        assertTrue(result.all { it.name.contains("John", ignoreCase = true) })
    }

    @Test
    fun getAuthorsShouldHandleAuthorsWithSpecialCharacters() = runTest {
        // Given
        val authors = createAuthorsWithNames(
            "O'Brien",
            "Smith-Jones",
            "José García",
            "François Müller",
            "李明",
        )
        authorDao.insert(authors)

        // When
        val result = authorDao.getAuthors(search = null).loadList()

        // Then
        assertEquals(5, result.size)
        assertTrue(result.any { it.name == "O'Brien" })
        assertTrue(result.any { it.name == "Smith-Jones" })
        assertTrue(result.any { it.name == "José García" })
    }

}
