package me.anasmusa.learncast.data.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import me.anasmusa.learncast.core.appConfig
import kotlin.time.Instant

expect fun createHttpClient(
    block: HttpClientConfig<*>.() -> Unit,
): HttpClient

internal fun HttpClientConfig<*>.configure(
    getTokenManager: () -> TokenManager,
    setExplicitNulls: Boolean = true,
) {
    expectSuccess = true

    install(Logging) {
        level = LogLevel.ALL
        logger =
            object : Logger {
                override fun log(message: String) {
                    Napier.v(message)
                }
            }
    }

    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 30_000
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                if (setExplicitNulls) {
                    // With explicitNulls enabled/disabled, missing optional fields may still throw MissingFieldException in unit tests
                    explicitNulls = false
                }
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
                serializersModule =
                    SerializersModule {
                        contextual(Instant::class, InstantSerializer)
                    }
            },
        )
    }

    install(Auth) {
        bearer {
            loadTokens {
                getTokenManager().getTokens()?.let {
                    BearerTokens(
                        accessToken = it.second,
                        refreshToken = it.first,
                    )
                }
            }
            refreshTokens {
                if (oldTokens?.refreshToken == null) return@refreshTokens null
                getTokenManager().refreshToken(oldTokens!!.refreshToken!!)?.let {
                    BearerTokens(
                        accessToken = it.second,
                        refreshToken = it.first,
                    )
                }
            }
        }
    }

    defaultRequest {
        header("Content-Type", "application/json")
        header("Accept-Language", "uz")
        url(appConfig.apiBaseUrl)
    }
}
