package me.anasmusa.learncast.data.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
internal fun HttpResponse.isCache() = Clock.System.now().toEpochMilliseconds() - responseTime.timestamp > 2_000

@OptIn(ExperimentalTime::class)
internal suspend inline fun <reified T> HttpResponse.bodyIfNotCache(): T? =
    if (isCache()) {
        null
    } else {
        body<T>()
    }
