package me.anasmusa.learncast.data.network.model.lesson

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ListenSessionResponse(
    @SerialName("listen_count") val listenCount: Long,
)
