package me.anasmusa.learncast.data.local.preference

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.PreferenceData

const val DATA_STORE_FILE_NAME = "preference_data.pb"

interface DataStoreFactory {
    fun create(): DataStore<PreferenceData>
}

interface Preferences {
    suspend fun updateToken(
        refreshToken: String,
        accessToken: String,
    )

    fun getToken(): Flow<Pair<String, String>?>

    suspend fun updateUser(user: PreferenceData.User)

    fun getUser(): Flow<PreferenceData.User?>

    suspend fun setLang(lang: String)

    fun getLang(): Flow<String?>

    suspend fun clear()
}
