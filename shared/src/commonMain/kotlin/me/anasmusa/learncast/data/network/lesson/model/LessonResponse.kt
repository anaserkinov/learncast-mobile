package me.anasmusa.learncast.data.network.lesson.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.data.network.author.model.AuthorResponse
import me.anasmusa.learncast.data.network.common.model.FileResponse
import me.anasmusa.learncast.data.network.topic.model.TopicResponse
import kotlin.time.Instant

@Serializable
class LessonResponse(
    val id: Long,
    val title: String,
    val description: String?,
    @SerialName("cover_image_path") val coverImagePath: String?,
    val author: AuthorResponse,
    val topic: TopicResponse.Topic?,
    val audio: FileResponse,
    @SerialName("listen_count") val listenCount: Long,
    @SerialName("snip_count") val snipCount: Long,
    @SerialName("created_at") val createdAt: Instant,
    @SerialName("is_favourite") val isFavourite: Boolean? = null,
    @SerialName("lesson_progress") val progress: LessonProgressResponse?,
)
