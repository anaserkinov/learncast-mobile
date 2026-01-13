package me.anasmusa.learncast.data.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
class DeletedResponse(
    val id: Long,
    @SerialName("deleted_at") val deletedAt: Instant
)