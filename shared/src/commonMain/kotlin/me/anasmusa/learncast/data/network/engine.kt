package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig

expect fun HttpClient(
    block: HttpClientConfig<*>.() -> Unit
): HttpClient