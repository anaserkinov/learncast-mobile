package me.anasmusa.learncast.data.network.model.topic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.data.network.model.author.AuthorResponse
import kotlin.time.Instant

@Serializable
class TopicResponse(
    val topic: Topic,
    val author: AuthorResponse,
    @SerialName("completed_lesson_count") val completedLessonCount: Long,
) {
    @Serializable
    class Topic(
        val id: Long,
        val title: String,
        val description: String?,
        @SerialName("cover_image_path") val coverImagePath: String?,
        @SerialName("created_at") val createdAt: Instant,
        @SerialName("lesson_count") val lessonCount: Long,
        @SerialName("total_duration") val totalDuration: Long,
    )
}
