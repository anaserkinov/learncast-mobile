package me.anasmusa.learncast.data.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.core.nowLocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
data class Topic(
    val id: Long,
    val topicId: Long,
    val title: String,
    val description: String?,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val createdAt: LocalDateTime,
    val lessonCount: Long,
    val totalDuration: Duration,
    val completedLessonCount: Long
)

fun getSampleTopic() = Topic(
    0L,
    0L,
    "Topic",
    "Description dgfebgeqb tbtrb wryt bnwyt bywt bwytn bybwytb trwrtwyb yw5 hnyjnynbrtbrtwnyn",
    null,
    0L,
    "Author",
    nowLocalDateTime(),
    10L,
    10.toDuration(DurationUnit.MINUTES),
    5L
)