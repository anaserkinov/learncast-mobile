package me.anasmusa.learncast.data.repository.abstraction

import me.anasmusa.learncast.data.model.ReferenceType

interface DownloadRepository {
    suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        title: String,
        lessonId: Long,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    )

    suspend fun remove(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
    )

    suspend fun removeAllDownloads()
}
