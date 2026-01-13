package me.anasmusa.learncast.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class UserProgressStatus {
    @SerialName("not_started") NOT_STARTED,
    @SerialName("in_progress") IN_PROGRESS,
    @SerialName("completed") COMPLETED
}