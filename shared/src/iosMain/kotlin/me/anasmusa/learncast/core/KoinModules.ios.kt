package me.anasmusa.learncast.core

import me.anasmusa.learncast.core.download.DownloadManager
import me.anasmusa.learncast.core.download.downloadManagerFactory
import me.anasmusa.learncast.core.google.GoogleAuthManager
import me.anasmusa.learncast.core.google.googleAuthManagerFactory
import org.koin.core.module.Module

actual fun Module.platformModule() {
    factory<GoogleAuthManager> {
        googleAuthManagerFactory.invoke()
    }

    factory<DownloadManager> {
        downloadManagerFactory.invoke()
    }
}
