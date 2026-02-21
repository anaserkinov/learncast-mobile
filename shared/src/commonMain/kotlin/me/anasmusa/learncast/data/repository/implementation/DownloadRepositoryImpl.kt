package me.anasmusa.learncast.data.repository.implementation

import me.anasmusa.learncast.core.download.DownloadManager
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository

internal class DownloadRepositoryImpl(
    private val downloadManager: DownloadManager,
    private val downloadDao: DownloadDao,
) : DownloadRepository {
    override suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        title: String,
        lessonId: Long,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    ) {
        try {
            val downloadState = downloadDao.get(referenceId, referenceUuid, referenceType)
            when (downloadState?.state) {
                DownloadState.COMPLETED -> return
                DownloadState.DOWNLOADING -> {
                    downloadManager.ensureDownloading(
                        downloadState.id,
                        downloadState.audioPath.normalizeUrl(),
                        downloadState.audioPath,
                        title,
                    )
                    return
                }
                DownloadState.STOPPED -> {
                    if (downloadManager.resumeDownload(downloadState.id)) {
                        return
                    } else {
                        remove(
                            downloadState.referenceId,
                            downloadState.referenceUuid,
                            downloadState.referenceType,
                        )
                    }
                }

                else -> {}
            }
            createDownloadRequest(
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
                title = title,
                audioPath = audioPath,
                startMs = startMs,
                endMs = endMs,
            )
        } catch (e: Exception) {
        }
    }

    private suspend fun createDownloadRequest(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        title: String,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    ) {
        val id =
            downloadDao.insert(
                DownloadStateEntity(
                    id = 0L,
                    referenceId = referenceId,
                    referenceUuid = referenceUuid,
                    referenceType = referenceType,
                    audioPath = audioPath,
                    startMs = startMs,
                    endMs = endMs,
                    state = DownloadState.DOWNLOADING,
                    percentDownloaded = 0f,
                ),
            )
        downloadManager.startDownload(
            id = id,
            url = audioPath.normalizeUrl(),
            audioPath = audioPath,
            title = title,
        )
    }

    override suspend fun remove(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
    ) {
        try {
            downloadDao.get(referenceId, referenceUuid, referenceType)?.let {
                downloadDao.delete(it.id)
                if (!downloadDao.isInUse(it.audioPath)) {
                    downloadManager.removeDownload(it.id, it.audioPath)
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun removeAllDownloads() {
        try {
            downloadManager.clear()
            downloadDao.clear()
        } catch (e: Exception) {
        }
    }
}
