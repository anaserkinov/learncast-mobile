package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.cache.HttpCache
import org.koin.mp.KoinPlatform

actual fun HttpClient(
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient =
    HttpClient(OkHttp) {
        engine {}
        install(HttpCache) {
            privateStorage(KoinPlatform.getKoin().get<CachingCacheStorage>())
        }
        block()
    }
