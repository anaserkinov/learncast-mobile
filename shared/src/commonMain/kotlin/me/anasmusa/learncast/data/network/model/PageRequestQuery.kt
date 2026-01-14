package me.anasmusa.learncast.data.network.model

import io.ktor.http.URLBuilder
import kotlinx.serialization.json.Json
import me.anasmusa.learncast.data.model.QueryOrder
import me.anasmusa.learncast.data.model.QuerySort
import me.anasmusa.learncast.data.model.UserProgressStatus

class PageRequestQuery(
    val limit: Int = 50,
    var cursor: String? = null,
    val search: String? = null,
    val favourite: Boolean? = null,
    val status: UserProgressStatus? = null,
    val sort: QuerySort? = null,
    val order: QueryOrder? = null,
    val authorId: Long? = null,
    val topicId: Long? = null,
    val lessonId: Long? = null,
) {
    fun toStringKey() =
        buildString {
            append(search).append(':')
            append(favourite).append(':')
            append(status).append(':')
            append(sort).append(':')
            append(order).append(':')
            append(authorId).append(':')
            append(topicId).append(':')
            append(lessonId)
        }

    fun load(url: URLBuilder) {
        url.apply {
            parameters.append("limit", limit.toString())
            if (cursor != null) {
                parameters.append("cursor", cursor.toString())
            }
            if (!search.isNullOrBlank()) {
                parameters.append("search", search)
            }
            if (favourite != null) {
                parameters.append("favourite", favourite.toString())
            }
            if (status != null) {
                parameters.append("status", Json.encodeToString(status))
            }
            if (sort != null) {
                parameters.append("sort", sort.value)
            }
            if (order != null) {
                parameters.append("order", order.value)
            }
            if (authorId != null) {
                parameters.append("author_id", authorId.toString())
            }
            if (topicId != null) {
                parameters.append("topic_id", topicId.toString())
            }
            if (lessonId != null) {
                parameters.append("lesson_id", lessonId.toString())
            }
        }
    }
}
