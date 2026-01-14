package me.anasmusa.learncast.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import me.anasmusa.learncast.screen.SearchScreen
import me.anasmusa.learncast.screen.auth.LoginScreen
import me.anasmusa.learncast.screen.author.AuthorListScreen
import me.anasmusa.learncast.screen.author.AuthorScreen
import me.anasmusa.learncast.screen.home.HomeScreen
import me.anasmusa.learncast.screen.profile.ProfileScreen
import me.anasmusa.learncast.screen.profile.StorageUsageScreen
import me.anasmusa.learncast.screen.snip.SnipListScreen
import me.anasmusa.learncast.screen.topic.TopicListScreen
import me.anasmusa.learncast.screen.topic.TopicScreen

fun entryProvider(): (NavKey) -> NavEntry<NavKey> =
    entryProvider {
        val noAnimation =
            NavDisplay.transitionSpec {
                EnterTransition.None togetherWith ExitTransition.None
            } +
                NavDisplay.popTransitionSpec {
                    EnterTransition.None togetherWith ExitTransition.None
                } +
                NavDisplay.predictivePopTransitionSpec {
                    EnterTransition.None togetherWith ExitTransition.None
                }
        entry<Screen.Entrance> { }
        entry<Screen.Login> { LoginScreen() }

        entry<Screen.Home>(
            metadata = noAnimation,
        ) { HomeScreen() }
        entry<Screen.Snips>(
            metadata = noAnimation,
        ) { SnipListScreen() }
        entry<Screen.Profile>(
            metadata = noAnimation,
        ) { ProfileScreen() }

        entry<Screen.TopicList> {
            TopicListScreen()
        }
        entry<Screen.Topic> {
            TopicScreen(it.topic)
        }

        entry<Screen.AuthorList> {
            AuthorListScreen()
        }
        entry<Screen.Author> {
            AuthorScreen(it.author)
        }

        entry<Screen.Search>(
            metadata =
                NavDisplay.transitionSpec {
                    fadeIn(initialAlpha = 0.5f) + scaleIn(initialScale = 0.8f) togetherWith ExitTransition.None
                } +
                    NavDisplay.popTransitionSpec {
                        EnterTransition.None togetherWith fadeOut(targetAlpha = 0f)
                    } +
                    NavDisplay.predictivePopTransitionSpec {
                        EnterTransition.None togetherWith fadeOut(targetAlpha = 0.5f) + scaleOut(targetScale = 0.8f)
                    },
        ) {
            SearchScreen(
                it.authorId,
                it.topicId,
                it.selectedTab,
            )
        }

        entry<Screen.StorageUsageScreen> {
            StorageUsageScreen()
        }
    }
