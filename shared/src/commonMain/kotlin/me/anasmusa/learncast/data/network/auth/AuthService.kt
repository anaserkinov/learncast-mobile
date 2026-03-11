package me.anasmusa.learncast.data.network.auth

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.auth.AuthCircuitBreaker
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import me.anasmusa.learncast.data.network.auth.model.Credentials
import me.anasmusa.learncast.data.network.auth.model.LoginRequest
import me.anasmusa.learncast.data.network.auth.model.LoginResponse
import me.anasmusa.learncast.data.network.auth.model.RefreshTokenRequest
import me.anasmusa.learncast.data.network.common.model.BaseResponse

internal class AuthService(
    private val client: HttpClient,
) {
    companion object {
        const val LOGIN = "v1/user/auth/login"
        const val REFRESH_TOKEN = "v1/user/auth/refresh-token"
        const val LOGOUT = "v1/user/auth/logout"
    }

    suspend fun login(request: LoginRequest) =
        client
            .post(LOGIN) {
                setBody(request)
            }.body<BaseResponse<LoginResponse>>()

    suspend fun refreshToken(request: RefreshTokenRequest) =
        client
            .post(REFRESH_TOKEN) {
                attributes.put(AuthCircuitBreaker, Unit)
                setBody(request)
            }.body<BaseResponse<Credentials>>()

    /**
     * accessToken is passed explicitly since AuthCircuitBreaker disables
     * automatic token injection by the Auth (Bearer) plugin for this request.
     */
    suspend fun logout(accessToken: String) {
        client
            .authProvider<BearerAuthProvider>()
            ?.clearToken()
        client.post(LOGOUT) {
            attributes.put(AuthCircuitBreaker, Unit)
            headers.append(HttpHeaders.Authorization, "Bearer $accessToken")
        }
    }
}
