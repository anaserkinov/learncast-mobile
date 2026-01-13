package me.anasmusa.learncast.data.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.core.nowLocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class Lesson(
    val id: Long,
    val title: String,
    val description: String?,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val topicId: Long?,
    val topicTitle: String?,
    val audioPath: String,
    val audioSize: Long,
    val audioDuration: Duration,
    val listenCount: Long,
    val snipCount: Long,
    val createdAt: LocalDateTime,
    val isFavourite: Boolean,
    val startedAt: LocalDateTime?,
    val lastPositionMs: Duration?,
    val status: UserProgressStatus,
    val completedAt: LocalDateTime?
)

fun getSampleLesson() = Lesson(
    id = 1,
    title = "Lesson",
    description = "description",
    coverImagePath = null,
    authorId = 2,
    authorName = "Author",
    topicId = 3,
    topicTitle = "Topic",
    createdAt = nowLocalDateTime(),
    audioPath = "audio",
    audioSize = 2434L,
    audioDuration = 4.toDuration(DurationUnit.MINUTES),
    listenCount = 1000L,
    snipCount = 2321,
    isFavourite = true,
    startedAt = nowLocalDateTime(),
    lastPositionMs = 0.toDuration(DurationUnit.MILLISECONDS),
    status = UserProgressStatus.IN_PROGRESS,
    completedAt = null
)