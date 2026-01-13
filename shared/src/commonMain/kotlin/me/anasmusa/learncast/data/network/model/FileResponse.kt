package me.anasmusa.learncast.data.network.model

import kotlinx.serialization.Serializable

@Serializable
class FileResponse(
    val path: String,
    val size: Long,
    val duration: Long
)