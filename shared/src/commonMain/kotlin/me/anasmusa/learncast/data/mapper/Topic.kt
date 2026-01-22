package me.anasmusa.learncast.data.mapper

import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.data.local.db.topic.TopicEntity
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.network.model.topic.TopicResponse
import kotlin.time.DurationUnit
import kotlin.time.toDuration

internal fun TopicResponse.toEntity() =
    TopicEntity(
        id = topic.id,
        title = topic.title,
        description = topic.description,
        coverImagePath = topic.coverImagePath,
        authorId = author.id,
        authorName = author.name,
        createdAt = topic.createdAt.toDateTime(),
        lessonCount = topic.lessonCount,
        totalDuration = topic.totalDuration.toDuration(DurationUnit.MILLISECONDS),
        completedLessonCount = completedLessonCount,
    )

internal fun TopicEntity.toUI() =
    Topic(
        id = id,
        title = title,
        description = description,
        coverImagePath = coverImagePath,
        authorId = authorId,
        authorName = authorName,
        createdAt = createdAt,
        lessonCount = lessonCount,
        totalDuration = totalDuration,
        completedLessonCount = completedLessonCount,
    )
