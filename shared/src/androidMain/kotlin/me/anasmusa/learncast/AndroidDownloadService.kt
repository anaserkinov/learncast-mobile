package me.anasmusa.learncast

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import androidx.media3.exoplayer.workmanager.WorkManagerScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.runBlocking
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.core.player.HttpDataSourceFactory
import me.anasmusa.learncast.data.DownloadCacheScope
import me.anasmusa.learncast.data.model.DownloadState
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named

@OptIn(UnstableApi::class)
class AndroidDownloadService :
    DownloadService(
        2,
        1000,
        "media-download",
        appConfig.downloadNotificationTitle,
        appConfig.downloadNotificationMessage,
    ),
    KoinComponent {
    private fun Int.toDownloadState() =
        when (this) {
            Download.STATE_QUEUED, Download.STATE_DOWNLOADING, Download.STATE_RESTARTING -> DownloadState.DOWNLOADING
            Download.STATE_STOPPED, Download.STATE_FAILED -> DownloadState.STOPPED
            Download.STATE_COMPLETED -> DownloadState.COMPLETED
            else -> DownloadState.REMOVING
        }

    override fun getDownloadManager(): DownloadManager {
        val downloadManager =
            DownloadManager(
                this,
                get(),
                get(named(DownloadCacheScope.ID)),
                HttpDataSourceFactory(get()),
                Dispatchers.IO.asExecutor(),
            )
        downloadManager.maxParallelDownloads = 1

        val downloadRepository = get<DownloadRepository>()
        downloadManager.addListener(
            object : DownloadManager.Listener {
                override fun onDownloadChanged(
                    downloadManager: DownloadManager,
                    download: Download,
                    finalException: Exception?,
                ) {
                    super.onDownloadChanged(downloadManager, download, finalException)
                    runBlocking {
                        downloadRepository.update(
                            download.request.id.toLong(),
                            download.state.toDownloadState(),
                            download.percentDownloaded,
                        )
                    }
                }

                override fun onDownloadRemoved(
                    downloadManager: DownloadManager,
                    download: Download,
                ) {
                    super.onDownloadRemoved(downloadManager, download)
                    runBlocking {
                        downloadRepository.remove(download.request.id.toLong())
                    }
                }
            },
        )

        return downloadManager
    }

    override fun getScheduler(): Scheduler = WorkManagerScheduler(this, "download_worker")

    override fun getForegroundNotification(
        downloads: List<Download>,
        notMetRequirements: Int,
    ): Notification =
        DownloadNotificationHelper(this, "media-download")
            .buildProgressNotification(
                this,
                appConfig.transparentLogo,
                null,
                null,
                downloads,
                notMetRequirements,
            )

    override fun onDestroy() {
        super.onDestroy()
    }
}
