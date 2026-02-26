package me.anasmusa.learncast.core

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import me.anasmusa.learncast.core.download.AndroidDownloadManager
import me.anasmusa.learncast.core.download.DownloadManager
import me.anasmusa.learncast.core.google.AndroidGoogleAuthManager
import me.anasmusa.learncast.core.google.GoogleAuthManager
import me.anasmusa.learncast.core.player.AndroidAudioPlayer
import me.anasmusa.learncast.core.player.AndroidPlayerController
import me.anasmusa.learncast.core.player.AudioPlayer
import me.anasmusa.learncast.core.player.PlayerController
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module

@OptIn(UnstableApi::class)
actual fun Module.platformModule() {
    factory<GoogleAuthManager> {
        AndroidGoogleAuthManager(androidContext())
    }

    factory<DownloadManager> {
        AndroidDownloadManager(androidContext())
    }

    factory<PlayerController> {
        AndroidPlayerController(androidContext())
    }

    factory<AudioPlayer> {
        AndroidAudioPlayer(androidContext(), it[0], it[1])
    }
}
