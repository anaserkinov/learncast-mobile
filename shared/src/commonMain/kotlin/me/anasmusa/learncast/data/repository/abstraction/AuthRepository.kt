package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.model.Result

interface AuthRepository {
    suspend fun loginWithTelegram(hash: String): Result<Unit>

    suspend fun loginWithGoogle(): Result<Unit>

    suspend fun logout()

    fun isLoggedIn(): Flow<Boolean>
}
