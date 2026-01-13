package me.anasmusa.learncast.data.network.model.lesson

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.data.model.UserProgressStatus
import kotlin.time.Instant

@Serializable
class LessonProgressResponse(
    @SerialName("lesson_id") val lessonId: Long,
    @SerialName("started_at") val startedAt: Instant,
    @SerialName("last_position_ms") val lastPositionMs: Long,
    val status: UserProgressStatus,
    @SerialName("completed_at") val completedAt: Instant?
)