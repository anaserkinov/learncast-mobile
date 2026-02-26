package me.anasmusa.learncast.data

import me.anasmusa.learncast.core.player.avplayer.cache.AVCache
import me.anasmusa.learncast.core.player.avplayer.cache.CacheIndex
import me.anasmusa.learncast.core.player.avplayer.cache.MetadataIndex
import me.anasmusa.learncast.data.local.preference.DataStoreFactory
import me.anasmusa.learncast.data.local.preference.IosDataStoreFactory
import me.anasmusa.learncast.data.local.storage.IosStorageManager
import me.anasmusa.learncast.data.local.storage.StorageManager
import me.anasmusa.learncast.data.network.CachingCacheStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named

internal actual fun Module.platformModule() {
    single<CachingCacheStorage> {
        CachingCacheStorage()
    }

    factoryOf(::MetadataIndex)

    factory(named(PlaybackCacheScope.ID)) {
        CacheIndex(AVCache.TABLE_CACHE, get())
    }

    factory(named(DownloadCacheScope.ID)) {
        CacheIndex(AVCache.TABLE_DOWNLOAD, get())
    }

    factory<DataStoreFactory> {
        IosDataStoreFactory()
    }

    factory<StorageManager> {
        IosStorageManager()
    }
}
