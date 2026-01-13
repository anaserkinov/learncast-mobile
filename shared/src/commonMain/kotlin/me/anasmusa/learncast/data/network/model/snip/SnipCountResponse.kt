package me.anasmusa.learncast.data.network.model.snip

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SnipCountResponse(
    @SerialName("lesson_id") val lessonId: Long,
    @SerialName("user_snip_count") val count: Long
)