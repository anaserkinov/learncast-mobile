package me.anasmusa.learncast.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer

fun createTestHttpClient(
    engineConfig: MockEngineConfig.() -> Unit
) = HttpClient(
    MockEngine.create(engineConfig)
) {
    configure(
        getTokenManager = { throw NotImplementedError() },
        setExplicitNulls = false
    )
    installOrReplace(Auth) {
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
}
