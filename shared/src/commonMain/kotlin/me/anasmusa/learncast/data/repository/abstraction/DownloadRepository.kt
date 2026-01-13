package me.anasmusa.learncast.data.repository.abstraction

import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType

interface DownloadRepository {

    suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        audioPath: String,
        startMs: Long?,
        endMs: Long?
    )

    suspend fun cancel(id: Long)
    suspend fun cancel(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType
    )

    suspend fun update(
        id: Long,
        state: DownloadState,
        percentDownloaded: Float
    )

    suspend fun removeAllDownloads()

}