package me.anasmusa.learncast.core

import me.anasmusa.learncast.core.google.GoogleAuthManager
import me.anasmusa.learncast.core.google.createGoogleAuthManager
import me.anasmusa.learncast.core.notification.NotificationManager
import me.anasmusa.learncast.core.notification.createNotificationManager
import me.anasmusa.learncast.data.AppScope
import me.anasmusa.learncast.data.repository.abstraction.PlayerRepository
import org.koin.dsl.module

internal fun coreModule() =
    module {
        factory<GoogleAuthManager> {
            createGoogleAuthManager()
        }

        factory<NotificationManager> {
            createNotificationManager()
        }

        factory<PlayerRepository> {
            getOrCreateScope<AppScope>(AppScope.ID).get()
        }
    }
