package me.anasmusa.learncast.data.network

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
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
import me.anasmusa.learncast.core.InstantSerializer
import me.anasmusa.learncast.core.appConfig
import me.anasmusa.learncast.data.network.service.AuthService
import me.anasmusa.learncast.data.network.service.AuthorService
import me.anasmusa.learncast.data.network.service.LessonService
import me.anasmusa.learncast.data.network.service.SnipService
import me.anasmusa.learncast.data.network.service.TopicService
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun networkModule() =
    module {
        single<HttpClient> {
            me.anasmusa.learncast.data.network.HttpClient {
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
                            explicitNulls = false
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
                            getKoin().get<TokenManager>().getTokens()?.let {
                                BearerTokens(
                                    accessToken = it.second,
                                    refreshToken = it.first,
                                )
                            }
                        }
                        refreshTokens {
                            if (oldTokens?.refreshToken == null) return@refreshTokens null
                            getKoin().get<TokenManager>().refreshToken(oldTokens!!.refreshToken!!)?.let {
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
                    url(appConfig.baseUrl)
                }
            }
        }

        single {
            TokenManager(get(), get(), get())
        }
        services()
    }

private fun Module.services() {
    factoryOf(::AuthService)
    factoryOf(::TopicService)
    factoryOf(::AuthorService)
    factoryOf(::LessonService)
    factoryOf(::SnipService)
}
