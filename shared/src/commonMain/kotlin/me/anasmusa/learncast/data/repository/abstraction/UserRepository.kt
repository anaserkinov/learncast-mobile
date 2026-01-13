package me.anasmusa.learncast.data.repository.abstraction

import me.anasmusa.learncast.data.model.Result
import me.anasmusa.learncast.data.model.User

interface UserRepository {
    suspend fun getUser(): Result<User>
}