package me.anasmusa.learncast.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource

@OptIn(UnstableApi::class)
internal fun createCacheDataSourceFactory(
    downloadCache: Cache,
    playbackCache: Cache,
    httpDataSourceFactory: HttpDataSourceFactory
) = CacheDataSource.Factory()
    .setCache(downloadCache)
    .setUpstreamDataSourceFactory(
        CacheDataSource.Factory()
            .setCache(playbackCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
    )
    .setCacheWriteDataSinkFactory(null)