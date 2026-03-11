package me.anasmusa.learncast.data

import android.database.sqlite.SQLiteDatabase
import android.os.Environment
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import me.anasmusa.learncast.core.getOrCreateScope
import me.anasmusa.learncast.data.local.db.AndroidDatabaseBuilder
import me.anasmusa.learncast.data.local.db.DatabaseBuilder
import me.anasmusa.learncast.data.local.preference.AndroidDataStoreFactory
import me.anasmusa.learncast.data.local.preference.DataStoreFactory
import me.anasmusa.learncast.data.local.storage.AndroidStorageManager
import me.anasmusa.learncast.data.local.storage.StorageManager
import me.anasmusa.learncast.data.network.CachingCacheStorage
import me.anasmusa.learncast.data.network.FileStorage
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import java.io.File

@OptIn(UnstableApi::class)
internal actual fun Module.platformModule() {
    single<SQLiteDatabase> {
        SQLiteDatabase.openOrCreateDatabase(
            androidContext().getDatabasePath("app.db"),
            null,
        )
    }

    single<DatabaseProvider> {
        object : DatabaseProvider {
            override fun getWritableDatabase(): SQLiteDatabase = get<SQLiteDatabase>()

            override fun getReadableDatabase(): SQLiteDatabase = get<SQLiteDatabase>()
        }
    }

    scope<PlaybackCacheScope> {
        scoped<Cache> {
            val cacheDir = androidContext().externalCacheDir!!.apply { mkdirs() }
            SimpleCache(
                File(cacheDir, "player").apply { mkdir() },
                LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024),
                getKoin().get<DatabaseProvider>(),
            )
        }
    }
    factory(named(PlaybackCacheScope.ID)) {
        getOrCreateScope<PlaybackCacheScope>(PlaybackCacheScope.ID).get<Cache>()
    }

    scope<DownloadCacheScope> {
        scoped<Cache> {
            SimpleCache(
                (androidContext().getExternalFilesDir(Environment.DIRECTORY_PODCASTS)!!).apply { mkdirs() },
                NoOpCacheEvictor(),
                getKoin().get<DatabaseProvider>(),
            )
        }
    }
    factory(named(DownloadCacheScope.ID)) {
        getOrCreateScope<DownloadCacheScope>(DownloadCacheScope.ID).get<Cache>()
    }

    single<CachingCacheStorage> {
        val cacheDir = androidContext().externalCacheDir!!.apply { mkdirs() }
        FileStorage(
            File(cacheDir, "http"),
        )
    }

    factory<DataStoreFactory> {
        AndroidDataStoreFactory(androidContext())
    }

    factory<StorageManager> {
        AndroidStorageManager(androidContext())
    }

    factory<DatabaseBuilder> {
        AndroidDatabaseBuilder(androidContext())
    }
}
