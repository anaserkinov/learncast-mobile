package me.anasmusa.learncast.data.network.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LoginResponse(
    val user: UserResponse,
    val credentials: Credentials
)

@Serializable
class UserResponse(
    val id: Long,
    @SerialName("first_name") val firstName: String,
    @SerialName("last_name") val lastName: String?,
    @SerialName("avatar_path") val avatarPath: String?,
    val email: String?,
    @SerialName("telegram_username") val telegramUsername: String?
)

@Serializable
class Credentials(
    @SerialName("refresh_token") val refreshToken: String,
    @SerialName("access_token") val accessToken: String,
)