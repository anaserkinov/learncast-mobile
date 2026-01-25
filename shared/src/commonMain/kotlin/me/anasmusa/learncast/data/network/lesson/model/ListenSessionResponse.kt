package me.anasmusa.learncast.data.network.lesson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class ListenSessionResponse(
    @SerialName("listen_count") val listenCount: Long,
)
