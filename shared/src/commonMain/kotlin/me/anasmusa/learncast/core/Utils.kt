package me.anasmusa.learncast.core

import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.network.model.BaseResponse
import org.koin.core.scope.Scope
import org.koin.core.scope.ScopeID

const val STATE_LOADING = 0
const val STATE_PLAYING = 1
const val STATE_PAUSED = 2

const val EVENT_SHOW_PLAYER = 1

suspend fun Exception.toResult(): Result.Fail {
    printStackTrace()
    return when (this) {
        is CancellationException -> throw this
        is IOException ->
            Result.Fail("Network Error")
        is ResponseException -> {
            try {
                val errorResponse = this.response.body<BaseResponse<Nothing?>>()
                return Result.Fail(errorResponse.message ?: "Response Error")
            } catch (e: Exception) {
                e.printStackTrace()
            }
            Result.Fail("Response Error")
        }
        else ->
            Result.Fail("Unknown Error")
    }
}

fun String.normalizeUrl() = "${appConfig.apiBaseUrl}/v1/file/$this"

inline fun <reified T : Any> Scope.getOrCreateScope(scopeId: ScopeID): Scope = getKoin().getOrCreateScope<T>(scopeId)
