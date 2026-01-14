package me.anasmusa.learncast.data.mapper

import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.data.local.db.lesson.LessonEntity
import me.anasmusa.learncast.data.local.db.lesson.LessonProgressInput
import me.anasmusa.learncast.data.local.db.lesson.LessonStateInput
import me.anasmusa.learncast.data.local.db.lesson.LessonWithState
import me.anasmusa.learncast.data.model.Lesson
import me.anasmusa.learncast.data.model.QueueItem
import me.anasmusa.learncast.data.model.ReferenceType
import me.anasmusa.learncast.data.model.UserProgressStatus
import me.anasmusa.learncast.data.network.model.lesson.LessonProgressResponse
import me.anasmusa.learncast.data.network.model.lesson.LessonResponse
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun LessonResponse.toEntity() =
    LessonEntity(
        id = id,
        title = title,
        description = description,
        coverImagePath = coverImagePath,
        authorId = author.id,
        authorName = author.name,
        topicId = topic?.id,
        topicTitle = topic?.title,
        audioPath = audio.path,
        audioSize = audio.size,
        audioDuration =
            audio.duration
                .toDuration(DurationUnit.MILLISECONDS)
                .inWholeSeconds
                .toDuration(DurationUnit.SECONDS),
        createdAt = createdAt.toDateTime(),
    )

internal fun LessonResponse.toInput() =
    LessonStateInput(
        lessonId = id,
        listenCount = listenCount,
        snipCount = snipCount,
        isFavourite = isFavourite!!,
        startedAt = progress?.startedAt?.toDateTime(),
        lastPositionMs = progress?.lastPositionMs?.toDuration(DurationUnit.MILLISECONDS),
        status = progress?.status ?: UserProgressStatus.NOT_STARTED,
        completedAt = progress?.completedAt?.toDateTime(),
    )

internal fun LessonProgressResponse.toInput() =
    LessonProgressInput(
        lessonId = lessonId,
        startedAt = startedAt.toDateTime(),
        lastPositionMs = lastPositionMs.toDuration(DurationUnit.MILLISECONDS),
        status = status,
        completedAt = completedAt?.toDateTime(),
    )

internal fun LessonWithState.toUI() =
    Lesson(
        id = lesson.id,
        title = lesson.title,
        description = lesson.description,
        coverImagePath = lesson.coverImagePath,
        authorId = lesson.authorId,
        authorName = lesson.authorName,
        topicId = lesson.topicId,
        topicTitle = lesson.topicTitle,
        audioPath = lesson.audioPath,
        audioSize = lesson.audioSize,
        audioDuration = lesson.audioDuration,
        createdAt = lesson.createdAt,
        listenCount = state.listenCount,
        snipCount = state.snipCount,
        isFavourite = state.isFavourite,
        startedAt = state.startedAt,
        lastPositionMs = state.lastPositionMs,
        status = state.status,
        completedAt = state.completedAt,
    )

fun Lesson.toQueueItem() =
    QueueItem(
        id = 0,
        referenceId = id,
        referenceUuid = "",
        referenceType = ReferenceType.LESSON,
        startMs = null,
        endMs = null,
        lessonId = id,
        title = title,
        description = description,
        coverImagePath = coverImagePath,
        authorId = authorId,
        authorName = authorName,
        topicId = topicId,
        topicTitle = topicTitle,
        audioPath = audioPath,
        audioSize = audioSize,
        audioDuration = audioDuration,
        lastPositionMs = lastPositionMs,
        status = status,
        isFavourite = isFavourite,
        downloadState = null,
        percentDownloaded = 0F,
    )
