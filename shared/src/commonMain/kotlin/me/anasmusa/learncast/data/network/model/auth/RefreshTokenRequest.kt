package me.anasmusa.learncast.data.network.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RefreshTokenRequest(
    @SerialName("refresh_token") val refreshToken: String
)