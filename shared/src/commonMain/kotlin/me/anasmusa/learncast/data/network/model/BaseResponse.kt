package me.anasmusa.learncast.data.network.model

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
class BaseResponse<out T>(
    val data: T,
    val message: String?,
    val time: Instant
)