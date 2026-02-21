/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package me.anasmusa.learncast.data.network

import io.ktor.client.plugins.cache.storage.CacheStorage
import io.ktor.client.plugins.cache.storage.CachedResponseData
import io.ktor.http.Url
import io.ktor.util.collections.ConcurrentMap

internal class CachingCacheStorage : CacheStorage {
    private val store = ConcurrentMap<Url, Set<CachedResponseData>>()

    override suspend fun store(
        url: Url,
        data: CachedResponseData,
    ) {
        if (store[url] == null) {
            store[url] = HashSet()
        }
        (store[url] as HashSet).add(data)
    }

    override suspend fun find(
        url: Url,
        varyKeys: Map<String, String>,
    ): CachedResponseData? {
        val data = store[url] ?: return null
        return data.find {
            varyKeys.all { (key, value) -> it.varyKeys[key] == value }
        }
    }

    override suspend fun findAll(url: Url): Set<CachedResponseData> = store[url] ?: emptySet()

    override suspend fun remove(
        url: Url,
        varyKeys: Map<String, String>,
    ) {
        (store[url] as? HashSet)?.removeAll {
            varyKeys.all { (key, value) -> it.varyKeys[key] == value }
        }
    }

    override suspend fun removeAll(url: Url) {
        store.remove(url)
    }

    suspend fun clear() {
        store.clear()
    }
}
