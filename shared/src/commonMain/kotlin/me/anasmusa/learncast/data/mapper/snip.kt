package me.anasmusa.learncast.data.mapper

import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.data.local.db.lesson.LessonEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonStateEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonWithState
import me.anasmusa.learncast.data.local.db.queue_item.QueueItemEntity
import me.anasmusa.learncast.data.local.db.snip.SnipEntity
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.Snip
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.model.lesson.LessonResponse
import me.anasmusa.learncast.data.network.model.snip.SnipResponse
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun SnipResponse.toEntity() = SnipEntity(
    id = id,
    clientSnipId = clientSnipId,
    startMs = startMs,
    endMs = endMs,
    note = note,
    createdAt = createdAt.toDateTime(),
    lessonId = lesson.id,
    title = lesson.title,
    description = lesson.description,
    coverImagePath = lesson.coverImagePath,
    authorId = lesson.author.id,
    authorName = lesson.author.name,
    topicId = lesson.topic?.id,
    topicTitle = lesson.topic?.title,
    audioPath = lesson.audio.path,
    audioSize = lesson.audio.size,
    audioDuration = lesson.audio.duration.toDuration(DurationUnit.MILLISECONDS)
)

internal fun SnipEntity.toUI() = Snip(
    id = id,
    clientSnipId = clientSnipId,
    startMs = startMs,
    endMs = endMs,
    note = note,
    createdAt = createdAt,
    lessonId = lessonId,
    title = title,
    coverImagePath = coverImagePath,
    authorId = authorId,
    authorName = authorName,
    topicId = topicId,
    topicTitle = topicTitle,
    audioPath = audioPath,
    audioSize = audioSize,
    audioDuration = audioDuration
)


fun Snip.toQueueItem() = QueueItem(
    id = 0,
    referenceId = 0,
    referenceUuid = clientSnipId,
    referenceType = ReferenceType.SNIP,
    startMs = startMs,
    endMs = endMs,
    lessonId = id,
    title = if (!note.isNullOrBlank())
        "$note - $title"
    else
        title,
    description = null,
    coverImagePath = coverImagePath,
    authorId = authorId,
    authorName = authorName,
    topicId = topicId,
    topicTitle = topicTitle,
    audioPath = audioPath,
    audioSize = audioSize,
    audioDuration = audioDuration,
    lastPositionMs = 0.toDuration(DurationUnit.MILLISECONDS),
    status = UserProgressStatus.NOT_STARTED,
    isFavourite = false,
    downloadState = null,
    percentDownloaded = 0F
)