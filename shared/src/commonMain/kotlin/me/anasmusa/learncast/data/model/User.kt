package me.anasmusa.learncast.data.model

data class User(
    val id: Long,
    val firstName: String,
    val lastName: String?,
    val avatarPath: String?,
    val email: String?,
    val telegramUsername: String?,
)
