package me.anasmusa.learncast.data.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HeadersImpl

internal fun HttpResponse.isCache() = headers is HeadersImpl
internal suspend inline fun <reified T> HttpResponse.bodyIfNotCache(): T? {
    return if (isCache()) null
    else body<T>()
}