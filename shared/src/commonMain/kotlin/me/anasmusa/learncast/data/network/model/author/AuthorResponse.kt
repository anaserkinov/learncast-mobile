package me.anasmusa.learncast.data.network.model.author

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuthorResponse(
    val id: Long,
    val name: String,
    @SerialName("avatar_path") val avatarPath: String?,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("lesson_count") val lessonCount: Long
)