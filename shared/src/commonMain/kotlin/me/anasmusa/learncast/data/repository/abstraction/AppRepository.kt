package me.anasmusa.learncast.data.repository.abstraction

import kotlinx.coroutines.flow.Flow

interface AppRepository {
    fun getLang(): Flow<String?>

    fun isNightMode(): Flow<Boolean?>

    suspend fun setNightMode(enabled: Boolean)
}
