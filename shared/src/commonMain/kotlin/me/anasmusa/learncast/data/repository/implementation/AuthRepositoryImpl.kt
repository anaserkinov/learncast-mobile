package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.PreferenceData
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.google.GoogleAuthManager
import me.anasmusa.learncast.core.notification.NotificationManager
import me.anasmusa.learncast.core.resource.Resource.string
import me.anasmusa.learncast.core.toResult
import me.anasmusa.learncast.data.AuthorizedUserScope
import me.anasmusa.learncast.data.local.db.DBConnection
import me.anasmusa.learncast.data.local.preference.Preferences
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.network.TokenManager
import me.anasmusa.learncast.data.network.auth.AuthService
import me.anasmusa.learncast.data.network.auth.model.LoginRequest
import me.anasmusa.learncast.data.network.auth.model.LoginResponse
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import me.anasmusa.learncast.data.repository.abstraction.StorageRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal class AuthRepositoryImpl(
    private val authService: AuthService,
    private val preference: Preferences,
    private val storageRepository: StorageRepository,
    private val playerRepository: PlayerRepository,
    private val googleAuthManager: GoogleAuthManager,
    private val notificationManager: NotificationManager,
    private val dbConnection: DBConnection,
) : AuthRepository,
    KoinComponent {
    private val tokenManager by inject<TokenManager>()

    private suspend fun handleResponse(response: LoginResponse) {
        preference.updateUser(
            PreferenceData.User(
                response.user.id,
                response.user.firstName,
                response.user.lastName,
                response.user.avatarPath,
                response.user.email,
                response.user.telegramUsername,
            ),
        )
        preference.updateToken(response.credentials.refreshToken, response.credentials.accessToken)
        notificationManager.subscribe()
        playerRepository.startService(false)
    }

    override suspend fun loginWithTelegram(hash: String): Result<Unit> {
        return try {
            val result =
                authService
                    .login(
                        LoginRequest(
                            telegramData = hash,
                        ),
                    ).data
            handleResponse(result)
            return Result.Success(Unit)
        } catch (e: Exception) {
            e.toResult()
        }
    }

    override suspend fun loginWithGoogle(): Result<Unit> {
        return try {
            val tokenId = googleAuthManager.signIn() ?: return Result.Fail(Strings.UNKNOWN_ERROR.string())
            val result =
                authService
                    .login(
                        LoginRequest(
                            googleData = tokenId,
                        ),
                    ).data
            handleResponse(result)
            Result.Success(Unit)
        } catch (e: Exception) {
            e.toResult()
        }
    }

    override suspend fun logout() =
        withContext(Dispatchers.IO) {
            try {
                playerRepository.clearQueue(true)
                playerRepository.stopService()
                getKoin().getScopeOrNull(AuthorizedUserScope.ID)?.close()

                tokenManager.cancelRefresh()
                preference.getToken().take(1).first()?.second?.let { accessToken ->
                    try {
                        authService.logout(accessToken = accessToken)
                    } catch (e: Exception) {
                    }
                }
                storageRepository.clearCaches()
                storageRepository.clearDownloads()
                notificationManager.unSubscribe()
                dbConnection.clearAllTables()
                preference.clear()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    override fun isLoggedIn(): Flow<Boolean> =
        preference.getUser().map {
            it != null
        }
}
