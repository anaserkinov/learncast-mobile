package me.anasmusa.learncast.data.local;

import androidx.datastore.core.DataStore
import me.anasmusa.learncast.PreferenceData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

const val DATA_STORE_FILE_NAME = "preference_data.pb"
expect fun getDataStore(): DataStore<PreferenceData>

class Preferences{
    private val dataStore = getDataStore()

    suspend fun updateToken(
        refreshToken: String,
        accessToken: String
    ) {
        dataStore.updateData {
            it.copy(
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        }
    }

    fun getToken(): Flow<Pair<String, String>?>{
        return dataStore.data.map {
            if(it.refreshToken != null && it.accessToken != null)
                Pair(it.refreshToken, it.accessToken)
            else null
        }
    }

    suspend fun updateUser(user: PreferenceData.User) {
        dataStore.updateData {
            it.copy(
                user = user
            )
        }
    }

    fun getUser(): Flow<PreferenceData.User?> {
        return dataStore.data.map {
            it.user
        }
    }

    suspend fun setLang(lang: String) {
        dataStore.updateData {
            it.copy(lang = lang)
        }
    }

    fun getLang(): Flow<String?> {
        return dataStore.data.map {
            it.lang
        }
    }

    suspend fun setNightMode(enabled: Boolean) {
        dataStore.updateData {
            it.copy(isNightMode = enabled)
        }
    }

    fun isNightMode(): Flow<Boolean?> {
        return dataStore.data.map {
            it.isNightMode
        }
    }

    suspend fun clear() {
        dataStore.updateData {
            it.copy(
                refreshToken = null,
                accessToken = null,
                user = null
            )
        }
    }
}