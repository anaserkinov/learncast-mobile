package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin
import io.ktor.client.plugins.cache.HttpCache
import org.koin.mp.KoinPlatform

actual fun createHttpClient(
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(Darwin) {
        engine {}
        install(HttpCache) {
            privateStorage(KoinPlatform.getKoin().get<CachingCacheStorage>())
        }
        block()
    }
