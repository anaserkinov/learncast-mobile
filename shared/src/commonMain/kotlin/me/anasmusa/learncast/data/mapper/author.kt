package me.anasmusa.learncast.data.mapper

import me.anasmusa.learncast.core.toDateTime
import me.anasmusa.learncast.data.local.db.author.AuthorEntity
import me.anasmusa.learncast.data.local.db.topic.TopicEntity
import me.anasmusa.learncast.data.model.Author
import me.anasmusa.learncast.data.model.Topic
import me.anasmusa.learncast.data.network.model.author.AuthorResponse
import me.anasmusa.learncast.data.network.model.topic.TopicResponse

internal fun AuthorResponse.toEntity() = AuthorEntity(
    id = id,
    name = name,
    avatarPath = avatarPath,
    createdAt = createdAt.toDateTime(),
    lessonCount = lessonCount
)

internal fun AuthorEntity.toUI() = Author(
    id = id,
    name = name,
    avatarPath = avatarPath,
    lessonCount = lessonCount
)