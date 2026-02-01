package me.anasmusa.learncast.ui

import me.anasmusa.learncast.core.getOrCreateScope
import me.anasmusa.learncast.data.AuthorizedUserScope
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
import org.koin.core.module.dsl.scopedOf
import org.koin.dsl.module

internal fun uiModule() =
    module {
        factoryOf(::AppViewModel)
        factoryOf(::PlayerViewModel)
        factoryOf(::LoginViewModel)

        scope<AuthorizedUserScope> {
            scopedOf(::HomeViewModel)
            scopedOf(::SnipListViewModel)
            scopedOf(::ProfileViewModel)
        }
        factory<HomeViewModel> {
            getOrCreateScope<AuthorizedUserScope>(AuthorizedUserScope.ID).get()
        }
        factory<SnipListViewModel> {
            getOrCreateScope<AuthorizedUserScope>(AuthorizedUserScope.ID).get()
        }
        factory<ProfileViewModel> {
            getOrCreateScope<AuthorizedUserScope>(AuthorizedUserScope.ID).get()
        }

        factoryOf(::TopicListViewModel)
        factoryOf(::TopicViewModel)

        factoryOf(::AuthorListViewModel)
        factoryOf(::AuthorViewModel)

        factoryOf(::SnipEditViewModel)

        factoryOf(::QueueViewModel)
        factoryOf(::PlayerSnipViewModel)

        factoryOf(::SearchViewModel)

        factoryOf(::StorageViewModel)
    }
