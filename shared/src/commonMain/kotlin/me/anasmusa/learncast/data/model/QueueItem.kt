package me.anasmusa.learncast.data.model

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

data class QueueItem(
    val id: Long,
    val referenceId: Long,
    val referenceUuid: String,
    val referenceType: ReferenceType,
    // snip
    val startMs: Long?,
    val endMs: Long?,
    // lesson
    val lessonId: Long,
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
    val lastPositionMs: Duration?,
    val status: UserProgressStatus,
    val isFavourite: Boolean,
    val downloadState: DownloadState?,
    val percentDownloaded: Float,
) {
    val subTitle =
        if (topicTitle != null) {
            "$authorName • $topicTitle"
        } else {
            authorName
        }
    val duration =
        if (startMs != null && endMs != null) {
            (endMs - startMs).toDuration(DurationUnit.MILLISECONDS)
        } else {
            audioDuration
        }
}

fun getSampleQueueItem(id: Long = 0L) =
    QueueItem(
        id = id,
        referenceId = 0,
        referenceUuid = "",
        referenceType = ReferenceType.LESSON,
        startMs = null,
        endMs = null,
        lessonId = 0,
        title = "title",
        description = null,
        coverImagePath = null,
        authorId = 0L,
        authorName = "Author",
        topicId = 0L,
        topicTitle = "Topic",
        audioPath = "",
        audioSize = 43243L,
        audioDuration = 90.toDuration(DurationUnit.MINUTES),
        lastPositionMs = null,
        status = UserProgressStatus.IN_PROGRESS,
        isFavourite = false,
        downloadState = null,
        percentDownloaded = 0f,
    )
