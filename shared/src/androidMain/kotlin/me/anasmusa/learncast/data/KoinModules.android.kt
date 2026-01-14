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
import me.anasmusa.learncast.ApplicationLoader
import me.anasmusa.learncast.core.getOrCreateScope
import me.anasmusa.learncast.data.network.CachingCacheStorage
import me.anasmusa.learncast.data.network.FileStorage
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import me.anasmusa.learncast.data.repository.implementation.DownloadRepositoryImpl
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import java.io.File

object PlaybackCacheScope {
    const val ID = "playback-cache-scope"
}

object DownloadCacheScope {
    const val ID = "download-cache-scope"
}

@OptIn(UnstableApi::class)
internal actual fun Module.platformModule() {
    single<SQLiteDatabase> {
        SQLiteDatabase.openOrCreateDatabase(
            ApplicationLoader.Companion.context.getDatabasePath("app.db"),
            null,
        )
    }

    single<DatabaseProvider> {
        object : DatabaseProvider {
            override fun getWritableDatabase(): SQLiteDatabase = get<SQLiteDatabase>()

            override fun getReadableDatabase(): SQLiteDatabase = get<SQLiteDatabase>()
        }
    }

    factory<DownloadRepository> {
        DownloadRepositoryImpl(
            androidContext(),
            get(),
            get(),
        )
    }

    scope<PlaybackCacheScope> {
        scoped<Cache> {
            val cacheDir = androidContext().externalCacheDir!!.apply { mkdirs() }
            SimpleCache(
                File(cacheDir, "player").apply { mkdir() },
                LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024),
                get<DatabaseProvider>(),
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
                get<DatabaseProvider>(),
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
}
