package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.flow.Flow
import me.anasmusa.learncast.data.local.Preferences
import me.anasmusa.learncast.data.repository.abstraction.AppRepository

internal class AppRepositoryImpl(
    private val preference: Preferences,
) : AppRepository {
    override fun getLang(): Flow<String?> = preference.getLang()
}
