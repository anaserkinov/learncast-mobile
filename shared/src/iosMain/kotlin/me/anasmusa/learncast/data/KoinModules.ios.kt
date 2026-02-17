package me.anasmusa.learncast.data

import me.anasmusa.learncast.core.player.PlayerController
import me.anasmusa.learncast.core.player.avplayer.cache.AVCache
import me.anasmusa.learncast.core.player.avplayer.cache.CacheIndex
import me.anasmusa.learncast.core.player.avplayer.cache.MetadataIndex
import me.anasmusa.learncast.core.player.createPlayer
import me.anasmusa.learncast.data.network.CachingCacheStorage
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.qualifier.named

internal actual fun Module.platformModule() {
    single<CachingCacheStorage> {
        CachingCacheStorage()
    }

    single<PlayerController> {
        createPlayer()
    }

    factoryOf(::MetadataIndex)

    factory(named(PlaybackCacheScope.ID)) {
        CacheIndex(AVCache.TABLE_CACHE, get())
    }

    factory(named(DownloadCacheScope.ID)) {
        CacheIndex(AVCache.TABLE_DOWNLOAD, get())
    }
}
