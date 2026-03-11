package me.anasmusa.learncast.data.local.storage

import android.content.Context
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.SimpleCache
import me.anasmusa.learncast.data.DownloadCacheScope
import me.anasmusa.learncast.data.PlaybackCacheScope
import me.anasmusa.learncast.data.network.CachingCacheStorage
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import java.io.File

@OptIn(UnstableApi::class)
class AndroidStorageManager(
    private val context: Context,
) : StorageManager,
    KoinComponent {
    private val databaseProvider by inject<DatabaseProvider>()

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
        get<Cache>(named(PlaybackCacheScope.ID)).release()
        SimpleCache.delete(File(cacheFolder, "player"), databaseProvider)
        getKoin().getScopeOrNull(PlaybackCacheScope.ID)?.close()
        clearHttpCaches()
    }

    private fun clearHttpCaches() {
        get<CachingCacheStorage>().clear()
    }

    override suspend fun clearDownloads() {
        val folder = context.getExternalFilesDir(Environment.DIRECTORY_PODCASTS) ?: return
        get<Cache>(named(DownloadCacheScope.ID)).release()
        SimpleCache.delete(folder, databaseProvider)
        getKoin().getScopeOrNull(DownloadCacheScope.ID)?.close()
    }
}
