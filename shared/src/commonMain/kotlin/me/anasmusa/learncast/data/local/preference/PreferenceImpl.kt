package me.anasmusa.learncast.data.local.preference

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.anasmusa.learncast.PreferenceData

internal class PreferenceImpl(
    dataStoreFactory: DataStoreFactory,
) : Preferences {
    private val dataStore = dataStoreFactory.create()

    override suspend fun updateToken(
        refreshToken: String,
        accessToken: String,
    ) {
        dataStore.updateData {
            it.copy(
                accessToken = accessToken,
                refreshToken = refreshToken,
            )
        }
    }

    override fun getToken(): Flow<Pair<String, String>?> =
        dataStore.data.map {
            if (it.refreshToken != null && it.accessToken != null) {
                Pair(it.refreshToken, it.accessToken)
            } else {
                null
            }
        }

    override suspend fun updateUser(user: PreferenceData.User) {
        dataStore.updateData {
            it.copy(
                user = user,
            )
        }
    }

    override fun getUser(): Flow<PreferenceData.User?> =
        dataStore.data.map {
            it.user
        }

    override suspend fun setLang(lang: String) {
        dataStore.updateData {
            it.copy(lang = lang)
        }
    }

    override fun getLang(): Flow<String?> =
        dataStore.data.map {
            it.lang
        }

    override suspend fun clear() {
        dataStore.updateData {
            it.copy(
                refreshToken = null,
                accessToken = null,
                user = null,
            )
        }
    }
}
