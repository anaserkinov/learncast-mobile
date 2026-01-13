package me.anasmusa.learncast.data.repository.implementation

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import me.anasmusa.learncast.AndroidDownloadService
import me.anasmusa.learncast.core.normalizeUrl
import me.anasmusa.learncast.data.local.db.DBConnection
import me.anasmusa.learncast.data.local.db.download.DownloadDao
import me.anasmusa.learncast.data.local.db.download.DownloadStateEntity
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.model.ReferenceType

@OptIn(UnstableApi::class)
internal class DownloadRepositoryImpl(
    private val context: Context,
    private val downloadDao: DownloadDao,
    private val dbConnection: DBConnection
) : DownloadRepository {

    override suspend fun download(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        audioPath: String,
        startMs: Long?,
        endMs: Long?
    ) {
        try {
            val downloadState = downloadDao.get(referenceId, referenceUuid, referenceType)
            when(downloadState?.state){
                DownloadState.DOWNLOADING, DownloadState.COMPLETED -> return
                DownloadState.STOPPED -> {
                    if (downloadState.parentId != 0L){
                        val lessonDownloadState = downloadDao.getById(downloadState.parentId)
                        if (lessonDownloadState != null){
                            downloadDao.update(
                                downloadState.id,
                                lessonDownloadState.state,
                                lessonDownloadState.percentDownloaded
                            )
                            if (lessonDownloadState.state == DownloadState.STOPPED)
                                DownloadService.sendRemoveDownload(
                                    context,
                                    AndroidDownloadService::class.java,
                                    lessonDownloadState.id.toString(),
                                    false
                                )
                        }
                        return
                    }
                    DownloadService.sendRemoveDownload(
                        context,
                        AndroidDownloadService::class.java,
                        downloadState.id.toString(),
                        false
                    )
                    return
                }
                else -> {}
            }
            val lessonDownloadState = downloadDao.getLessonState(referenceId)
            if (lessonDownloadState == null ||
                lessonDownloadState.state == DownloadState.REMOVING) {
                createDownloadRequest(
                    referenceId = referenceId,
                    referenceUuid = referenceUuid,
                    referenceType = referenceType,
                    audioPath = audioPath,
                    startMs = startMs,
                    endMs = endMs
                )
            } else {
                downloadDao.insert(
                    DownloadStateEntity(
                        id = 0L,
                        parentId = lessonDownloadState.id,
                        referenceId = referenceId,
                        referenceUuid = referenceUuid,
                        referenceType = referenceType,
                        audioPath = audioPath,
                        startMs = startMs,
                        endMs = endMs,
                        state = lessonDownloadState.state,
                        percentDownloaded = lessonDownloadState.percentDownloaded
                    )
                )
                if (lessonDownloadState.state == DownloadState.STOPPED)
                    DownloadService.sendRemoveDownload(
                        context,
                        AndroidDownloadService::class.java,
                        lessonDownloadState.id.toString(),
                        false
                    )
            }
        } catch (e: Exception){}
    }

    private suspend fun createDownloadRequest(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType,
        audioPath: String,
        startMs: Long?,
        endMs: Long?
    ){
        val id = downloadDao.insert(
            DownloadStateEntity(
                id = 0L,
                parentId = 0L,
                referenceId = referenceId,
                referenceUuid = referenceUuid,
                referenceType = referenceType,
                audioPath = audioPath,
                startMs = startMs,
                endMs = endMs,
                state = DownloadState.DOWNLOADING,
                percentDownloaded = 0f
            )
        )
        DownloadService.sendAddDownload(
            context,
            AndroidDownloadService::class.java,
            DownloadRequest.Builder(id.toString(), audioPath.normalizeUrl().toUri())
                .apply {
                    if (startMs != null && endMs != null)
                        setTimeRange(startMs * 1000, endMs * 1000)
                }
                .build(),
            false
        )
    }

    override suspend fun cancel(id: Long) {
        try {
            downloadDao.getById(id)?.let {
                cancel(id, it.referenceType)
            }
        } catch (e: Exception){}
    }

    override suspend fun cancel(
        referenceId: Long,
        referenceUuid: String,
        referenceType: ReferenceType
    ) {
        try {
            downloadDao.get(referenceId, referenceUuid, referenceType)?.let {
                cancel(it.id, referenceType)
                DownloadService.sendRemoveDownload(
                    context,
                    AndroidDownloadService::class.java,
                    it.id.toString(),
                    false
                )
            }
        } catch (e: Exception){}
    }

    private suspend fun cancel(
        id: Long,
        referenceType: ReferenceType
    ){
        dbConnection.inWriteTransaction {
            if (referenceType == ReferenceType.LESSON)
                downloadDao.getChildren(id).forEach {
                    createDownloadRequest(
                        it.referenceId,
                        it.referenceUuid,
                        it.referenceType,
                        it.audioPath,
                        it.startMs,
                        it.endMs
                    )
                }
            downloadDao.delete(id)
        }
    }

    override suspend fun update(id: Long, state: DownloadState, percentDownloaded: Float) {
        try {
            downloadDao.update(
                id,
                state,
                percentDownloaded
            )
        } catch (e: Exception){}
    }

    override suspend fun removeAllDownloads() {
        DownloadService.sendRemoveAllDownloads(
            context,
            AndroidDownloadService::class.java,
            false
        )
        downloadDao.clear()
    }
}