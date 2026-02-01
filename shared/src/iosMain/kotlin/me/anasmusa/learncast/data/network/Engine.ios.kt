package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun HttpClient(block: HttpClientConfig<*>.() -> Unit) =
    HttpClient(Darwin) {
        engine {}
//        install(HttpCache) {
//            privateStorage(KoinPlatform.getKoin().get<CachingCacheStorage>())
//        }
        block()
    }
