package me.anasmusa.learncast.data.network.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal class LoginRequest(
    val method: LoginMethod,
    val data: LoginData,
)

internal enum class LoginMethod {
    @SerialName("telegram")
    TELEGRAM,

    @SerialName("google")
    GOOGLE,
}

@Serializable
internal sealed interface LoginData {
    @Serializable
    class Telegram(
        @SerialName("id_token") val idToken: String,
    ) : LoginData

    @Serializable
    class Google(
        @SerialName("id_token") val idToken: String,
    ) : LoginData
}
