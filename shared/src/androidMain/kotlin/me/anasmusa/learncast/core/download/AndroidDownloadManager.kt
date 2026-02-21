package me.anasmusa.learncast.core.download

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import me.anasmusa.learncast.data.network.TokenProvider
import me.anasmusa.learncast.data.service.AndroidDownloadService

@OptIn(UnstableApi::class)
class AndroidDownloadManager(
    private val context: Context,
) : DownloadManager {
    override fun setTokenProvider(provider: TokenProvider) {
    }

    override fun startDownload(
        id: Long,
        url: String,
        audioPath: String,
        title: String,
    ) {
        DownloadService.sendAddDownload(
            context,
            AndroidDownloadService::class.java,
            DownloadRequest
                .Builder(id.toString(), url.toUri())
                .build(),
            false,
        )
    }

    override fun ensureDownloading(
        id: Long,
        url: String,
        audioPath: String,
        title: String,
    ) {
    }

    override fun resumeDownload(id: Long): Boolean {
        DownloadService.sendResumeDownloads(
            context,
            AndroidDownloadService::class.java,
            false,
        )
        return true
    }

    override fun removeDownload(
        id: Long,
        audioPath: String,
    ) {
        DownloadService.sendRemoveDownload(
            context,
            AndroidDownloadService::class.java,
            id.toString(),
            false,
        )
    }

    override fun clear() {
        DownloadService.clearDownloadManagerHelpers()
        DownloadService.sendRemoveAllDownloads(
            context,
            AndroidDownloadService::class.java,
            false,
        )
    }
}
