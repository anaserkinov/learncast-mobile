package me.anasmusa.learncast.data.repository.implementation

import me.anasmusa.learncast.data.local.Preferences
import me.anasmusa.learncast.data.repository.abstraction.AppRepository
import kotlinx.coroutines.flow.Flow


internal class AppRepositoryImpl(
    private val preference: Preferences
): AppRepository {

    override fun getLang(): Flow<String?> {
        return preference.getLang()
    }

    override fun isNightMode(): Flow<Boolean?> {
        return preference.isNightMode()
    }

    override suspend fun setNightMode(enabled: Boolean) {
        preference.setNightMode(enabled)
    }

}