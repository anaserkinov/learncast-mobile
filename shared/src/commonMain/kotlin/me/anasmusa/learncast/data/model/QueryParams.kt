package me.anasmusa.learncast.data.model

class QueryParams(
    val sort: QuerySort? = null,
    val order: QueryOrder? = null,
    val authorId: String? = null,
    val topicId: String? = null
)