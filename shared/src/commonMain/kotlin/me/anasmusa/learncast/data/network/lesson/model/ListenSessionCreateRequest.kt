package me.anasmusa.learncast.data.network.lesson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
class ListenSessionCreateRequest(
    @SerialName("session_id") val sessionId: String,
    @SerialName("created_at") val createdAt: Instant,
)
