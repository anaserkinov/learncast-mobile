package me.anasmusa.learncast.data.network

import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.data.local.preference.Preferences
import me.anasmusa.learncast.data.network.auth.AuthService
import me.anasmusa.learncast.data.network.auth.model.RefreshTokenRequest
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository

class TokenManager internal constructor(
    private val authService: AuthService,
    private val preferences: Preferences,
    private val authRepository: AuthRepository,
) {
    private val refreshMutex = Mutex()
    private var refreshJob: Job? = null

    suspend fun getTokens(): Pair<String, String>? = preferences.getToken().take(1).last()

    suspend fun refreshToken(refreshToken: String): Pair<String, String>? {
        val savedTokens = preferences.getToken().take(1).last()
        if (savedTokens != null && savedTokens.first != refreshToken) {
            return savedTokens
        }
        return refreshTokenUnsafe(refreshToken)
    }

    fun cancelRefresh() {
        refreshJob?.cancel()
        refreshJob = null
    }

    private suspend fun refreshTokenUnsafe(refreshToken: String): Pair<String, String>? {
        refreshMutex.withLock {
            refreshJob?.let {
                if (it.isActive || it.isCompleted) {
                    it.join()
                }
            }
            preferences.getToken().take(1).last()?.let {
                if (refreshToken != it.first) {
                    return it
                }
            }
            refreshJob =
                GlobalScope.launch {
                    try {
                        val tokens = authService.refreshToken(RefreshTokenRequest(refreshToken)).data
                        preferences.updateToken(tokens.refreshToken, tokens.accessToken)
                    } catch (e: Exception) {
                        if (e is ResponseException) {
                            if (e.response.status.value == 401) {
                                withContext(NonCancellable) {
                                    authRepository.logout()
                                }
                            }
                        }
                        e.printStackTrace()
                    }
                    refreshJob = null
                }
        }
        refreshJob?.join()
        return preferences.getToken().take(1).last()
    }
}
