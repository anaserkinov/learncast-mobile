package me.anasmusa.learncast.data.network.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class LoginRequest(
    @SerialName("telegram_data") val telegramData: String? = null,
    @SerialName("google_data") val googleData: String? = null,
)
