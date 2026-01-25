package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.HttpTimeoutConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import me.anasmusa.learncast.core.InstantSerializer
import me.anasmusa.learncast.core.appConfig
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun createHttpClient(
    engineConfig: MockEngineConfig.() -> Unit
) = HttpClient(
    MockEngine.create(engineConfig)
) {
    expectSuccess = true

    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = 60_000
        socketTimeoutMillis = 30_000
    }

    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
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
                BearerTokens(
                    accessToken = "AccessToken",
                    refreshToken = "RefreshToken",
                )
            }
            refreshTokens {
                if (oldTokens?.refreshToken == null) return@refreshTokens null
                BearerTokens(
                    accessToken = "AccessToken",
                    refreshToken = "RefreshToken",
                )
            }
        }
    }

    defaultRequest {
        header("Content-Type", "application/json")
        header("Accept-Language", "uz")
        url(appConfig.apiBaseUrl)
    }
}
