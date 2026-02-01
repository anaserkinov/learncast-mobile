package me.anasmusa.learncast.data.repository.implementation

import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository

internal class DownloadRepositoryImpl : DownloadRepository {
    override suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        lessonId: Long,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun remove(id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun remove(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun update(
        id: Long,
        state: DownloadState,
        percentDownloaded: Float,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun removeAllDownloads() {
        TODO("Not yet implemented")
    }
}
