package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

actual fun HttpClient(block: HttpClientConfig<*>.() -> Unit): HttpClient {
    TODO("Not yet implemented")
}
