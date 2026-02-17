package me.anasmusa.learncast.core

import me.anasmusa.learncast.core.notification.NotificationManager
import me.anasmusa.learncast.core.notification.createNotificationManager
import org.koin.core.module.Module
import org.koin.dsl.module

internal expect fun Module.platformModule()

internal fun coreModule() =
    module {
        platformModule()

        factory<NotificationManager> {
            createNotificationManager()
        }
    }
