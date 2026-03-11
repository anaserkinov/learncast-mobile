package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.Result

interface AuthRepository {
    suspend fun loginWithTelegram(
        id: Long,
        firstName: String,
        lastName: String?,
        username: String?,
        photoUrl: String?,
        authDate: Long,
        hash: String,
    ): Result<Unit>

    suspend fun loginWithGoogle(): Result<Unit>

    suspend fun logout()

    fun isLoggedIn(): Flow<Boolean>
}
