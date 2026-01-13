package me.anasmusa.learncast.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed class Screen: NavKey {

    @Serializable
    data object Entrance: Screen()

    @Serializable
    data object Login: Screen()

    @Serializable
    data object Home: Screen()
    @Serializable
    data object Snips: Screen()
    @Serializable
    data object Profile: Screen()

    @Serializable
    data object TopicList: Screen()
    @Serializable
    data class Topic(
        val topic: me.anasmusa.learncast.data.model.Topic
    ): Screen()

    @Serializable
    data object AuthorList: Screen()
    @Serializable
    data class Author(
        val author: me.anasmusa.learncast.data.model.Author
    ): Screen()

    @Serializable
    data class Search(
        val authorId: Long,
        val topicId: Long?,
        val selectedTab: Int = 0
    ): Screen()

    @Serializable
    data object StorageUsageScreen: Screen()

}