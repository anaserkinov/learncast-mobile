package me.anasmusa.learncast.core.download

import me.anasmusa.learncast.data.network.TokenProvider

interface DownloadManager {
    fun setTokenProvider(provider: TokenProvider)

    fun startDownload(
        id: Long,
        url: String,
        audioPath: String,
        title: String,
    )

    fun ensureDownloading(
        id: Long,
        url: String,
        audioPath: String,
        title: String,
    )

    fun resumeDownload(id: Long): Boolean

    fun removeDownload(
        id: Long,
        audioPath: String,
    )

    fun clear()
}
