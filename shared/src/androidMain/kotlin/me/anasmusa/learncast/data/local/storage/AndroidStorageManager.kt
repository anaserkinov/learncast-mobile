package me.anasmusa.learncast.data.local.storage

import android.content.Context
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.SimpleCache
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.data.DownloadCacheScope
import me.anasmusa.learncast.data.PlaybackCacheScope
import me.anasmusa.learncast.data.network.CachingCacheStorage
import org.koin.core.Koin
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform
import java.io.File

@OptIn(UnstableApi::class)
class AndroidStorageManager(
    private val context: Context,
    private val koin: Koin,
) : StorageManager {
    private val databaseProvider = koin.get<DatabaseProvider>()

    override suspend fun getCacheSize(): Float =
        (
            context.externalCacheDir
                ?.walkTopDown()
                ?.map { it.length() }
                ?.sum() ?: 0
        ) / (1024f * 1024f)

    override suspend fun getDownloadSize(): Float =
        (
            context
                .getExternalFilesDir(Environment.DIRECTORY_PODCASTS)
                ?.walkTopDown()
                ?.map { it.length() }
                ?.sum() ?: 0
        ) / (1024f * 1024f)

    override suspend fun clearCaches() {
        val cacheFolder = context.externalCacheDir ?: return
        koin.get<Cache>(named(PlaybackCacheScope.ID)).release()
        SimpleCache.delete(File(cacheFolder, "player"), databaseProvider)
        koin.getScopeOrNull(PlaybackCacheScope.ID)?.close()
        clearHttpCaches()
    }

    override suspend fun clearHttpCaches() {
        val cacheFolder = context.externalCacheDir ?: return
        File(cacheFolder, "http").deleteRecursively()
        koin.get<CachingCacheStorage>().clearMap()
    }

    override suspend fun clearDownloads() {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: return
        koin.get<Cache>(named(DownloadCacheScope.ID)).release()
        SimpleCache.delete(folder, databaseProvider)
        koin.getScopeOrNull(DownloadCacheScope.ID)?.close()
    }
}

actual fun createStorageManager(): StorageManager =
    AndroidStorageManager(
        ApplicationLoader.context,
        KoinPlatform.getKoin(),
    )
