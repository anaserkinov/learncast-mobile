package me.anasmusa.learncast.ui

import me.anasmusa.learncast.ui.auth.LoginViewModel
import me.anasmusa.learncast.ui.author.AuthorListViewModel
import me.anasmusa.learncast.ui.author.AuthorViewModel
import me.anasmusa.learncast.ui.home.HomeViewModel
import me.anasmusa.learncast.ui.player.PlayerViewModel
import me.anasmusa.learncast.ui.player.queue.QueueViewModel
import me.anasmusa.learncast.ui.player.snip.PlayerSnipViewModel
import me.anasmusa.learncast.ui.profile.ProfileViewModel
import me.anasmusa.learncast.ui.profile.StorageViewModel
import me.anasmusa.learncast.ui.snip.SnipEditViewModel
import me.anasmusa.learncast.ui.snip.SnipListViewModel
import me.anasmusa.learncast.ui.topic.TopicListViewModel
import me.anasmusa.learncast.ui.topic.TopicViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal fun uiModule() =
    module {
        factoryOf(::AppViewModel)
        factoryOf(::PlayerViewModel)
        factoryOf(::LoginViewModel)

        factoryOf(::HomeViewModel)

        factoryOf(::TopicListViewModel)
        factoryOf(::TopicViewModel)

        factoryOf(::AuthorListViewModel)
        factoryOf(::AuthorViewModel)

        factoryOf(::SnipListViewModel)
        factoryOf(::SnipEditViewModel)

        factoryOf(::QueueViewModel)
        factoryOf(::PlayerSnipViewModel)

        factoryOf(::SearchViewModel)

        factoryOf(::ProfileViewModel)
        factoryOf(::StorageViewModel)
    }
