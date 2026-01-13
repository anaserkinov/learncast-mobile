package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.anasmusa.learncast.PreferenceData
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.toResult
import me.anasmusa.learncast.data.google_auth.googleSignIn
import me.anasmusa.learncast.data.local.Preferences
import me.anasmusa.learncast.data.local.db.DBConnection
import me.anasmusa.learncast.data.local.storage.StorageManager
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.network.model.auth.LoginRequest
import me.anasmusa.learncast.data.network.model.auth.LoginResponse
import me.anasmusa.learncast.data.network.service.AuthService
import me.anasmusa.learncast.data.repository.abstraction.AuthRepository
import me.anasmusa.learncast.data.repository.abstraction.DownloadRepository
import me.anasmusa.learncast.string


internal class AuthRepositoryImpl(
    private val authService: AuthService,
    private val preference: Preferences,
    private val storageManager: StorageManager,
    private val downloadRepository: DownloadRepository,
    private val dbConnection: DBConnection
) : AuthRepository {

    private suspend fun handleResponse(response: LoginResponse){
        preference.updateUser(
            PreferenceData.User(
                response.user.id,
                response.user.firstName,
                response.user.lastName,
                response.user.avatarPath,
                response.user.email,
                response.user.telegramUsername
            )
        )
        preference.updateToken(response.credentials.refreshToken, response.credentials.accessToken)
    }

    override suspend fun loginWithTelegram(hash: String): Result<Unit> {
        return try {
            val result = authService.login(
                LoginRequest(
                    telegramData = hash
                )
            ).data
            handleResponse(result)
            return Result.Success(Unit)
        } catch (e: Exception){
            e.toResult()
        }
    }

    override suspend fun loginWithGoogle(): Result<Unit> {
        return try {
            val tokenId = googleSignIn() ?: return Result.Fail(Strings.unknown_error.string())
            val result = authService.login(
                LoginRequest(
                    googleData = tokenId
                )
            ).data
            handleResponse(result)
            Result.Success(Unit)
        } catch (e: Exception){
            e.toResult()
        }
    }

    override suspend fun logout() = withContext(Dispatchers.IO){
        try {
            try {
                authService.logout()
            } catch (e: Exception){}
            try {
                downloadRepository.removeAllDownloads()
            } catch (e: Exception){}
            storageManager.clearHttpCaches()
            dbConnection.clearAllTables()
            preference.clear()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun isLoggedIn(): Flow<Boolean> {
        return preference.getUser().map {
            it != null
        }
    }


}