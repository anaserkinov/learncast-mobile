package me.anasmusa.learncast.core

import me.anasmusa.learncast.core.download.AndroidDownloadManager
import me.anasmusa.learncast.core.download.DownloadManager
import me.anasmusa.learncast.core.google.AndroidGoogleAuthManager
import me.anasmusa.learncast.core.google.GoogleAuthManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module

actual fun Module.platformModule() {
    factory<GoogleAuthManager> {
        AndroidGoogleAuthManager()
    }

    factory<DownloadManager> {
        AndroidDownloadManager(androidContext())
    }
}
