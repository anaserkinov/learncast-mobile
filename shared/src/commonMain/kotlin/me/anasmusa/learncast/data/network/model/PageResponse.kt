package me.anasmusa.learncast.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class PageResponse<out T>(
    val items: List<T>,
    @SerialName("next_cursor") val nextCursor: String?
)