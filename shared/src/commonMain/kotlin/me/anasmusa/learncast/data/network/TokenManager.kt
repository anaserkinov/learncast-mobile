package me.anasmusa.learncast.data.network

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.anasmusa.learncast.data.local.Preferences
import me.anasmusa.learncast.data.network.model.auth.RefreshTokenRequest
import me.anasmusa.learncast.data.network.service.AuthService
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository

internal class TokenManager(
    private val authService: AuthService,
    private val preferences: Preferences,
    private val authRepository: AuthRepository
) {

    private val refreshMutex = Mutex()

    suspend fun getTokens(): Pair<String, String>? {
        return refreshMutex.withLock {
            preferences.getToken().take(1).last()
        }
    }

    suspend fun refreshToken(refreshToken: String): Pair<String, String>? {
        return refreshMutex.withLock {
            val savedTokens = preferences.getToken().take(1).last()
            if (savedTokens != null && savedTokens.first != refreshToken)
                return savedTokens
            refreshTokenUnsafe(refreshToken)
        }
    }

    private suspend fun refreshTokenUnsafe(refreshToken: String): Pair<String, String>? {
        return try {
            val tokens = authService.refreshToken(RefreshTokenRequest(refreshToken)).data
            preferences.updateToken(tokens.refreshToken, tokens.accessToken)
            Pair(tokens.refreshToken, tokens.accessToken)
        } catch (e: Exception) {
            if (e is ResponseException){
                if (e.response.status.value == 401)
                    authRepository.logout()
            }
            e.printStackTrace()
            null
        }
    }
}