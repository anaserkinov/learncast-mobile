package me.anasmusa.learncast.core

import me.anasmusa.learncast.core.download.DownloadManager
import me.anasmusa.learncast.core.download.downloadManagerFactory
import me.anasmusa.learncast.core.google.GoogleAuthManager
import me.anasmusa.learncast.core.google.googleAuthManagerFactory
import me.anasmusa.learncast.core.notification.IosNotificationManager
import me.anasmusa.learncast.core.notification.NotificationManager
import me.anasmusa.learncast.core.player.AudioPlayer
import me.anasmusa.learncast.core.player.IosAudioPlayer
import me.anasmusa.learncast.core.player.IosPlayerController
import me.anasmusa.learncast.core.player.PlayerController
import me.anasmusa.learncast.core.resource.IosResourceManager
import me.anasmusa.learncast.core.resource.ResourceManager
import org.koin.core.module.Module

actual fun Module.platformModule() {
    factory<GoogleAuthManager> {
        googleAuthManagerFactory.invoke()
    }

    factory<DownloadManager> {
        downloadManagerFactory.invoke()
    }

    factory<PlayerController> {
        IosPlayerController()
    }

    factory<AudioPlayer> {
        IosAudioPlayer(it[0], it[1])
    }

    factory<ResourceManager> {
        IosResourceManager()
    }

    factory<NotificationManager> {
        IosNotificationManager()
    }
}
