package me.anasmusa.learncast.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Author(
    val id: Long,
    val name: String,
    val avatarPath: String?,
    val lessonCount: Long,
)

fun getSampleAuthor() =
    Author(
        1L,
        "Author",
        null,
        10,
    )
