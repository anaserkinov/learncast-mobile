package me.anasmusa.learncast.data.network.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import me.anasmusa.learncast.data.network.model.BaseResponse
import me.anasmusa.learncast.data.network.model.auth.Credentials
import me.anasmusa.learncast.data.network.model.auth.LoginRequest
import me.anasmusa.learncast.data.network.model.auth.LoginResponse
import me.anasmusa.learncast.data.network.model.auth.RefreshTokenRequest

internal class AuthService(
    private val client: HttpClient,
) {
    suspend fun login(request: LoginRequest) =
        client
            .post("v1/user/auth/signin") {
                setBody(request)
            }.body<BaseResponse<LoginResponse>>()

    suspend fun refreshToken(request: RefreshTokenRequest) =
        client
            .post("v1/user/auth/refresh-token") {
                attributes.put(AuthCircuitBreaker, Unit)
                setBody(request)
            }.body<BaseResponse<Credentials>>()

    suspend fun logout() {
        client
            .authProvider<BearerAuthProvider>()
            ?.clearToken()
        client.post("v1/user/auth/logout") {
            attributes.put(AuthCircuitBreaker, Unit)
        }
    }
}
