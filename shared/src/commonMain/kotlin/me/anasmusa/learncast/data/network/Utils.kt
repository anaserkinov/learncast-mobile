package me.anasmusa.learncast.data.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlin.time.Clock

internal fun HttpResponse.isCache() = Clock.System.now().toEpochMilliseconds() - responseTime.timestamp > 2_000

internal suspend inline fun <reified T> HttpResponse.bodyIfNotCache(): T? =
    if (isCache()) {
        null
    } else {
        body<T>()
    }
