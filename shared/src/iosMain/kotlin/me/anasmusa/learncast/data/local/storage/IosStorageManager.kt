package me.anasmusa.learncast.data.local.storage

import kotlinx.cinterop.ExperimentalForeignApi
import me.anasmusa.learncast.core.player.avplayer.cache.CacheIndex
import me.anasmusa.learncast.data.DownloadCacheScope
import me.anasmusa.learncast.data.PlaybackCacheScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

class IosStorageManager :
    StorageManager,
    KoinComponent {
    private val cacheIndex by inject<CacheIndex>(named(PlaybackCacheScope.ID))
    private val downloadIndex by inject<CacheIndex>(named(DownloadCacheScope.ID))

    override suspend fun getCacheSize(): Float = cacheIndex.getTotalLength().toFloat() / (1024f * 1024f)

    override suspend fun getDownloadSize(): Float = downloadIndex.getTotalLength().toFloat() / (1024f * 1024f)

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearCaches() {
        deletedFolder(NSCachesDirectory, "audio")
        cacheIndex.clear()
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun clearDownloads() {
        deletedFolder(NSDocumentDirectory, "audio")
        downloadIndex.clear()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun deletedFolder(
        parentDir: NSSearchPathDirectory,
        folderName: String,
    ) {
        val fileManager = NSFileManager.defaultManager

        val cachesUrl =
            fileManager
                .URLsForDirectory(parentDir, NSUserDomainMask)
                .firstOrNull() as? NSURL ?: return

        val audioDirUrl =
            cachesUrl.URLByAppendingPathComponent(
                pathComponent = folderName,
                isDirectory = true,
            ) ?: return

        if (!fileManager.fileExistsAtPath(audioDirUrl.path!!)) return

        fileManager.removeItemAtURL(audioDirUrl, null)
    }
}
