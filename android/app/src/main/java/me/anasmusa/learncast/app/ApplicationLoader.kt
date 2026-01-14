package me.anasmusa.learncast.app

import me.anasmusa.learncast.R
import me.anasmusa.learncast.core.AppConfig
import me.anasmusa.learncast.lib.core.ApplicationLoader

class ApplicationLoader : ApplicationLoader() {
    override fun onCreate() {
        AppConfig.update(
            appName = "LearnCast",
            mainLogo = R.drawable.logo,
            loginLogo = R.drawable.logo_transparent,
            apiBaseUrl = "http://localhost:3000",
            publicBaseUrl = "https://learncast.anasmusa.me",
            telegramBotId = 8292515516L,
            telegramOrigin = "http://127.0.0.1:80",
            googleClientId = "22454749576-42ii04497d5aceqndkbvpnvn29nvub02.apps.googleusercontent.com",
        )
        super.onCreate()
    }
}
