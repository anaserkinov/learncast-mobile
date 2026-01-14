package me.anasmusa.learncast.data.model

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable
import me.anasmusa.learncast.core.nowLocalDateTime
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@Serializable
class Snip(
    val id: Long,
    val clientSnipId: String,
    val startMs: Long,
    val endMs: Long,
    val note: String?,
    val createdAt: LocalDateTime,
    // lesson
    val lessonId: Long,
    val title: String,
    val coverImagePath: String?,
    val authorId: Long,
    val authorName: String,
    val topicId: Long?,
    val topicTitle: String?,
    val audioPath: String,
    val audioSize: Long,
    val audioDuration: Duration,
) {
    val snipTitle by lazy {
        buildString {
            if (!note.isNullOrBlank()) {
                append(note)
                    .append(" - ")
            }
            append(title)
            if (topicTitle != null) {
                append(" - ")
                    .append(topicTitle)
            }
        }
    }
    val duration = (endMs - startMs).toDuration(DurationUnit.MILLISECONDS)
}

fun getSampleSnip(id: Long = 1) =
    Snip(
        id = id,
        clientSnipId = "fdgf",
        startMs = 0L,
        endMs = 124321L,
        note = "afdgfg",
        createdAt = nowLocalDateTime(),
        lessonId = 2,
        title = "Lesson",
        coverImagePath = null,
        authorId = 2,
        authorName = "Author",
        topicId = 3,
        topicTitle = "Topic",
        audioPath = "audio",
        audioSize = 2434L,
        audioDuration = 4.toDuration(DurationUnit.MINUTES),
    )
