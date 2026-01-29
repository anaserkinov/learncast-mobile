package me.anasmusa.learncast.data.local.db.download

import kotlinx.coroutines.test.runTest
import me.anasmusa.learncast.data.local.db.AppDatabase
import me.anasmusa.learncast.data.local.db.TestFixtures.Download.createDownloadState
import me.anasmusa.learncast.data.local.db.getInMemoryDatabase
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DownloadDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var downloadDao: DownloadDao

    @BeforeTest
    fun setup() {
        database = getInMemoryDatabase()
        downloadDao = database.getDownloadDao()
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    @Test
    fun insertShouldAddDownloadStateAndReturnId() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0, // Auto-generate
            referenceId = 1,
            referenceUuid = "uuid-1",
            audioPath = "/path/to/audio1.mp3"
        )

        // When
        val insertedId = downloadDao.insert(downloadState)

        // Then
        assertTrue(insertedId > 0)
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(insertedId, result.id)
    }

    @Test
    fun insertMultipleItemsShouldReturnDifferentIds() = runTest {
        // Given
        val downloadState1 = createDownloadState(id = 0, referenceId = 1, referenceUuid = "uuid-1")
        val downloadState2 = createDownloadState(id = 0, referenceId = 2, referenceUuid = "uuid-2")

        // When
        val id1 = downloadDao.insert(downloadState1)
        val id2 = downloadDao.insert(downloadState2)

        // Then
        assertNotEquals(id1, id2)
        assertTrue(id1 > 0)
        assertTrue(id2 > 0)
    }

    @Test
    fun getByIdShouldReturnCorrectEntity() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 123,
            referenceUuid = "test-uuid",
            audioPath = "/audio/test.mp3",
            state = DownloadState.DOWNLOADING,
            percentDownloaded = 50f
        )
        val insertedId = downloadDao.insert(downloadState)

        // When
        val result = downloadDao.getById(insertedId)

        // Then
        assertNotNull(result)
        assertEquals(insertedId, result.id)
        assertEquals(123L, result.referenceId)
        assertEquals("test-uuid", result.referenceUuid)
        assertEquals("/audio/test.mp3", result.audioPath)
        assertEquals(DownloadState.DOWNLOADING, result.state)
        assertEquals(50f, result.percentDownloaded)
    }

    @Test
    fun getByIdWithNonExistentIdShouldReturnNull() = runTest {
        // When
        val result = downloadDao.getById(999L)

        // Then
        assertNull(result)
    }

    @Test
    fun getShouldReturnEntityByReferenceIdUuidAndType() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 100,
            referenceUuid = "unique-uuid",
            referenceType = ReferenceType.LESSON,
            audioPath = "/lesson/audio.mp3"
        )
        downloadDao.insert(downloadState)

        // When
        val result = downloadDao.get(
            referenceId = 100,
            referenceUuid = "unique-uuid",
            referenceType = ReferenceType.LESSON
        )

        // Then
        assertNotNull(result)
        assertEquals(100L, result.referenceId)
        assertEquals("unique-uuid", result.referenceUuid)
        assertEquals(ReferenceType.LESSON, result.referenceType)
    }

    @Test
    fun getShouldReturnNullWhenNoMatch() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 100,
            referenceUuid = "uuid-1",
            referenceType = ReferenceType.LESSON
        )
        downloadDao.insert(downloadState)

        // When
        val result = downloadDao.get(
            referenceId = 100,
            referenceUuid = "different-uuid",
            referenceType = ReferenceType.LESSON
        )

        // Then
        assertNull(result)
    }

    @Test
    fun getShouldDistinguishBetweenReferenceTypes() = runTest {
        // Given
        val lesson = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-1",
            referenceType = ReferenceType.LESSON,
            audioPath = "/lesson.mp3"
        )
        val snip = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-1",
            referenceType = ReferenceType.SNIP,
            audioPath = "/snip.mp3"
        )
        downloadDao.insert(lesson)
        downloadDao.insert(snip)

        // When
        val lessonResult = downloadDao.get(1, "uuid-1", ReferenceType.LESSON)
        val snipResult = downloadDao.get(1, "uuid-1", ReferenceType.SNIP)

        // Then
        assertNotNull(lessonResult)
        assertNotNull(snipResult)
        assertEquals(ReferenceType.LESSON, lessonResult.referenceType)
        assertEquals(ReferenceType.SNIP, snipResult.referenceType)
        assertEquals("/lesson.mp3", lessonResult.audioPath)
        assertEquals("/snip.mp3", snipResult.audioPath)
    }

    @Test
    fun updateShouldModifyStateAndPercentDownloaded() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            state = DownloadState.DOWNLOADING,
            percentDownloaded = 25f
        )
        val insertedId = downloadDao.insert(downloadState)

        // When
        downloadDao.update(
            id = insertedId,
            state = DownloadState.COMPLETED,
            percentDownloaded = 100f
        )

        // Then
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(DownloadState.COMPLETED, result.state)
        assertEquals(100f, result.percentDownloaded)
    }

    @Test
    fun updateShouldNotModifyOtherFields() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 555,
            referenceUuid = "test-uuid",
            audioPath = "/original/path.mp3",
            state = DownloadState.DOWNLOADING,
            percentDownloaded = 0f
        )
        val insertedId = downloadDao.insert(downloadState)

        // When
        downloadDao.update(
            id = insertedId,
            state = DownloadState.STOPPED,
            percentDownloaded = 75f
        )

        // Then
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(555L, result.referenceId)
        assertEquals("test-uuid", result.referenceUuid)
        assertEquals("/original/path.mp3", result.audioPath)
        assertEquals(DownloadState.STOPPED, result.state)
        assertEquals(75f, result.percentDownloaded)
    }

    @Test
    fun updateWithNonExistentIdShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        downloadDao.update(
            id = 999L,
            state = DownloadState.COMPLETED,
            percentDownloaded = 100f
        )
    }

    @Test
    fun isInUseShouldReturnTrueWhenAudioPathExists() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            audioPath = "/shared/audio.mp3"
        )
        downloadDao.insert(downloadState)

        // When
        val result = downloadDao.isInUse("/shared/audio.mp3")

        // Then
        assertTrue(result)
    }

    @Test
    fun isInUseShouldReturnFalseWhenAudioPathDoesNotExist() = runTest {
        // When
        val result = downloadDao.isInUse("/nonexistent/audio.mp3")

        // Then
        assertFalse(result)
    }

    @Test
    fun isInUseShouldReturnTrueWhenMultipleEntriesShareSameAudioPath() = runTest {
        // Given
        val downloadState1 = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-1",
            audioPath = "/shared/audio.mp3"
        )
        val downloadState2 = createDownloadState(
            id = 0,
            referenceId = 2,
            referenceUuid = "uuid-2",
            audioPath = "/shared/audio.mp3"
        )
        downloadDao.insert(downloadState1)
        downloadDao.insert(downloadState2)

        // When
        val result = downloadDao.isInUse("/shared/audio.mp3")

        // Then
        assertTrue(result)
    }

    @Test
    fun isInUseShouldBeCaseSensitive() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            audioPath = "/Audio/File.mp3"
        )
        downloadDao.insert(downloadState)

        // When
        val result = downloadDao.isInUse("/audio/file.mp3")

        // Then
        assertFalse(result)
    }

    @Test
    fun deleteByIdShouldRemoveEntity() = runTest {
        // Given
        val downloadState = createDownloadState(id = 0)
        val insertedId = downloadDao.insert(downloadState)

        // When
        downloadDao.delete(insertedId)

        // Then
        val result = downloadDao.getById(insertedId)
        assertNull(result)
    }

    @Test
    fun deleteByIdWithNonExistentIdShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        downloadDao.delete(999L)
    }

    @Test
    fun deleteByIdShouldOnlyRemoveSpecifiedEntity() = runTest {
        // Given
        val downloadState1 = createDownloadState(id = 0, referenceId = 1)
        val downloadState2 = createDownloadState(id = 0, referenceId = 2)
        val id1 = downloadDao.insert(downloadState1)
        val id2 = downloadDao.insert(downloadState2)

        // When
        downloadDao.delete(id1)

        // Then
        assertNull(downloadDao.getById(id1))
        assertNotNull(downloadDao.getById(id2))
    }

    @Test
    fun deleteByAudioPathShouldRemoveEntity() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            audioPath = "/to/delete/audio.mp3"
        )
        downloadDao.insert(downloadState)

        // When
        downloadDao.delete("/to/delete/audio.mp3")

        // Then
        val result = downloadDao.isInUse("/to/delete/audio.mp3")
        assertFalse(result)
    }

    @Test
    fun deleteByAudioPathShouldRemoveAllMatchingEntries() = runTest {
        // Given
        val downloadState1 = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-1",
            audioPath = "/shared/audio.mp3"
        )
        val downloadState2 = createDownloadState(
            id = 0,
            referenceId = 2,
            referenceUuid = "uuid-2",
            audioPath = "/shared/audio.mp3"
        )
        val id1 = downloadDao.insert(downloadState1)
        val id2 = downloadDao.insert(downloadState2)

        // When
        downloadDao.delete("/shared/audio.mp3")

        // Then
        assertNull(downloadDao.getById(id1))
        assertNull(downloadDao.getById(id2))
        assertFalse(downloadDao.isInUse("/shared/audio.mp3"))
    }

    @Test
    fun deleteByAudioPathWithNonExistentPathShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        downloadDao.delete("/nonexistent/path.mp3")
    }

    @Test
    fun clearShouldRemoveAllEntities() = runTest {
        // Given
        val downloadState1 = createDownloadState(id = 0, referenceId = 1, referenceUuid = "uuid-1")
        val downloadState2 = createDownloadState(id = 0, referenceId = 2, referenceUuid = "uuid-2")
        val downloadState3 = createDownloadState(id = 0, referenceId = 3, referenceUuid = "uuid-3")
        val id1 = downloadDao.insert(downloadState1)
        val id2 = downloadDao.insert(downloadState2)
        val id3 = downloadDao.insert(downloadState3)

        // When
        downloadDao.clear()

        // Then
        assertNull(downloadDao.getById(id1))
        assertNull(downloadDao.getById(id2))
        assertNull(downloadDao.getById(id3))
    }

    @Test
    fun clearOnEmptyDatabaseShouldNotFail() = runTest {
        // When/Then - Should not throw exception
        downloadDao.clear()
    }

    @Test
    fun insertShouldHandleNullableFields() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            startMs = null,
            endMs = null
        )

        // When
        val insertedId = downloadDao.insert(downloadState)

        // Then
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertNull(result.startMs)
        assertNull(result.endMs)
    }

    @Test
    fun insertShouldHandleNonNullOptionalFields() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            startMs = 1000L,
            endMs = 5000L
        )

        // When
        val insertedId = downloadDao.insert(downloadState)

        // Then
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertEquals(1000L, result.startMs)
        assertEquals(5000L, result.endMs)
    }

    @Test
    fun updateShouldWorkAcrossDifferentStates() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            state = DownloadState.DOWNLOADING,
            percentDownloaded = 0f
        )
        val insertedId = downloadDao.insert(downloadState)

        // When/Then - Transition through states
        downloadDao.update(insertedId, DownloadState.DOWNLOADING, 25f)
        assertEquals(DownloadState.DOWNLOADING, downloadDao.getById(insertedId)?.state)

        downloadDao.update(insertedId, DownloadState.STOPPED, 25f)
        assertEquals(DownloadState.STOPPED, downloadDao.getById(insertedId)?.state)

        downloadDao.update(insertedId, DownloadState.DOWNLOADING, 75f)
        assertEquals(DownloadState.DOWNLOADING, downloadDao.getById(insertedId)?.state)

        downloadDao.update(insertedId, DownloadState.COMPLETED, 100f)
        assertEquals(DownloadState.COMPLETED, downloadDao.getById(insertedId)?.state)
        assertEquals(100f, downloadDao.getById(insertedId)?.percentDownloaded)

        downloadDao.update(insertedId, DownloadState.REMOVING, 0f)
        assertEquals(DownloadState.REMOVING, downloadDao.getById(insertedId)?.state)
    }

    @Test
    fun getShouldHandleMultipleEntriesWithSameReferenceId() = runTest {
        // Given
        val downloadState1 = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-1",
            referenceType = ReferenceType.LESSON
        )
        val downloadState2 = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-2",
            referenceType = ReferenceType.LESSON
        )
        downloadDao.insert(downloadState1)
        downloadDao.insert(downloadState2)

        // When
        val result1 = downloadDao.get(1, "uuid-1", ReferenceType.LESSON)
        val result2 = downloadDao.get(1, "uuid-2", ReferenceType.LESSON)

        // Then
        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals("uuid-1", result1.referenceUuid)
        assertEquals("uuid-2", result2.referenceUuid)
    }

    @Test
    fun isInUseShouldWorkAfterDeleteAndReinsert() = runTest {
        // Given
        val audioPath = "/test/audio.mp3"
        val downloadState = createDownloadState(id = 0, audioPath = audioPath)
        val insertedId = downloadDao.insert(downloadState)

        // When
        assertTrue(downloadDao.isInUse(audioPath))
        downloadDao.delete(insertedId)
        assertFalse(downloadDao.isInUse(audioPath))

        val newDownloadState = createDownloadState(id = 0, audioPath = audioPath, referenceId = 999)
        downloadDao.insert(newDownloadState)

        // Then
        assertTrue(downloadDao.isInUse(audioPath))
    }

    @Test
    fun insertShouldHandleSpecialCharactersInPaths() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            audioPath = "/path/with spaces/and-dashes/file's name (1).mp3"
        )

        // When
        val insertedId = downloadDao.insert(downloadState)

        // Then
        val result = downloadDao.getById(insertedId)
        assertNotNull(result)
        assertEquals("/path/with spaces/and-dashes/file's name (1).mp3", result.audioPath)
    }

    @Test
    fun getShouldHandleUnicodeInUuid() = runTest {
        // Given
        val downloadState = createDownloadState(
            id = 0,
            referenceId = 1,
            referenceUuid = "uuid-测试-テスト-🎵",
            referenceType = ReferenceType.LESSON
        )
        downloadDao.insert(downloadState)

        // When
        val result = downloadDao.get(1, "uuid-测试-テスト-🎵", ReferenceType.LESSON)

        // Then
        assertNotNull(result)
        assertEquals("uuid-测试-テスト-🎵", result.referenceUuid)
    }

    @Test
    fun updateShouldHandleEdgePercentageValues() = runTest {
        // Given
        val downloadState = createDownloadState(id = 0, percentDownloaded = 0f)
        val insertedId = downloadDao.insert(downloadState)

        // When/Then
        downloadDao.update(insertedId, DownloadState.DOWNLOADING, 0f)
        assertEquals(0f, downloadDao.getById(insertedId)?.percentDownloaded)

        downloadDao.update(insertedId, DownloadState.DOWNLOADING, 0.5f)
        assertEquals(0.5f, downloadDao.getById(insertedId)?.percentDownloaded)

        downloadDao.update(insertedId, DownloadState.COMPLETED, 100f)
        assertEquals(100f, downloadDao.getById(insertedId)?.percentDownloaded)
    }

}
