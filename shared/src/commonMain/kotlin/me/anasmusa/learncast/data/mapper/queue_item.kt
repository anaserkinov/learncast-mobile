package me.anasmusa.learncast.data.mapper

import me.anasmusa.learncast.data.local.db.queue_item.QueueItemEntity
import me.anasmusa.learncast.data.local.db.queue_item.QueueItemWithState
import me.anasmusa.learncast.data.model.QueueItem

internal fun QueueItemWithState.toUI() = QueueItem(
    id = item.id,
    referenceId = item.referenceId,
    referenceUuid = item.referenceUuid,
    referenceType = item.referenceType,
    startMs = item.startMs,
    endMs = item.endMs,
    lessonId = item.lessonId,
    title = item.title,
    description = item.description,
    coverImagePath = item.coverImagePath,
    authorId = item.authorId,
    authorName = item.authorName,
    topicId = item.topicId,
    topicTitle = item.topicTitle,
    audioPath = item.audioPath,
    audioSize = item.audioSize,
    audioDuration = item.audioDuration,
    lastPositionMs = state.lastPositionMs,
    status = state.status,
    isFavourite = state.isFavourite,
    downloadState = downloadState,
    percentDownloaded = percentDownloaded ?: 0F
)

internal fun QueueItem.toEntity() = QueueItemEntity(
    id = id,
    order = 0,
    referenceId = referenceId,
    referenceUuid = referenceUuid,
    referenceType = referenceType,
    startMs = startMs,
    endMs = endMs,
    lessonId = lessonId,
    title = title,
    description = description,
    coverImagePath = coverImagePath,
    authorId = authorId,
    authorName = authorName,
    topicId = topicId,
    topicTitle = topicTitle,
    audioPath = audioPath,
    audioSize = audioSize,
    audioDuration = audioDuration
)