package me.anasmusa.learncast.data.repository.implementation

import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.take
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.core.toResult
import me.anasmusa.learncast.data.local.Preferences
import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.model.User
import me.anasmusa.learncast.data.repository.abstraction.UserRepository

internal class UserRepositoryImpl(
    private val preferences: Preferences,
) : UserRepository {
    override suspend fun getUser(): Result<User> =
        try {
            preferences.getUser().take(1).last()?.let {
                Result.Success(
                    User(
                        it.id,
                        it.firstName,
                        it.lastName,
                        it.avatarPath,
                        it.email,
                        it.telegramUsername,
                    ),
                )
            } ?: Result.Fail(Strings.UNKNOWN_ERROR.string())
        } catch (e: Exception) {
            e.toResult()
        }
}
