package me.anasmusa.learncast.data.repository.implementation

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import me.anasmusa.learncast.AndroidDownloadService
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository

@OptIn(UnstableApi::class)
internal class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
) : DownloadRepository {
    override suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        lessonId: Long,
        audioPath: String,
        startMs: Long?,
        endMs: Long?,
    ) {
        try {
            val downloadState = downloadDao.get(referenceId, referenceUuid, referenceType)
            when (downloadState?.state) {
                DownloadState.DOWNLOADING, DownloadState.COMPLETED -> return
                DownloadState.STOPPED -> {
                    DownloadService.sendResumeDownloads(
                        context,
                        AndroidDownloadService::class.java,
                        false,
                    )
                    return
                }

                else -> {}
            }
            createDownloadRequest(
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
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
        DownloadService.sendAddDownload(
            context,
            AndroidDownloadService::class.java,
            DownloadRequest
                .Builder(id.toString(), audioPath.normalizeUrl().toUri())
                .build(),
            false,
        )
    }

    override suspend fun remove(id: Long) {
        try {
            downloadDao.getById(id)?.let {
                downloadDao.delete(it.audioPath)
            }
        } catch (e: Exception) {
        }
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
                    DownloadService.sendRemoveDownload(
                        context,
                        AndroidDownloadService::class.java,
                        it.id.toString(),
                        false,
                    )
                }
            }
        } catch (e: Exception) {
        }
    }

    override suspend fun update(
        id: Long,
        state: DownloadState,
        percentDownloaded: Float,
    ) {
        try {
            downloadDao.update(
                id,
                state,
                percentDownloaded,
            )
        } catch (e: Exception) {
        }
    }

    override suspend fun removeAllDownloads() {
        try {
            DownloadService.clearDownloadManagerHelpers()
            DownloadService.sendRemoveAllDownloads(
                context,
                AndroidDownloadService::class.java,
                false,
            )
            downloadDao.clear()
        } catch (e: Exception) {
        }
    }
}
