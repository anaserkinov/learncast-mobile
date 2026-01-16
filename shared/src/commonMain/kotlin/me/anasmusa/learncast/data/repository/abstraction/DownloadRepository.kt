package me.anasmusa.learncast.data.repository.abstraction

import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType

interface DownloadRepository {
    suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        lessonId: Long,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    )

    suspend fun remove(id: Long)

    suspend fun remove(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
    )

    suspend fun update(
        id: Long,
        state: DownloadState,
        percentDownloaded: Float,
    )

    suspend fun removeAllDownloads()
}
