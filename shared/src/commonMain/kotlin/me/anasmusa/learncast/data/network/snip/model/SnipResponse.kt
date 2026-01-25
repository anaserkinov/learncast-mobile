package me.anasmusa.learncast.data.network.snip.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.data.network.lesson.model.LessonResponse
import kotlin.time.Instant

@Serializable
class SnipResponse(
    val id: Long,
    @SerialName("client_snip_id") val clientSnipId: String,
    @SerialName("start_ms") val startMs: Long,
    @SerialName("end_ms") val endMs: Long,
    @SerialName("note_text") val note: String?,
    val lesson: LessonResponse,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("user_snip_count") val userSnipCount: Long?,
)
