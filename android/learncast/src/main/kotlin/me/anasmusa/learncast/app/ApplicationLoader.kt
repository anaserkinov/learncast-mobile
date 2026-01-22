package me.anasmusa.learncast.app

import me.anasmusa.learncast.R
import me.anasmusa.learncast.core.AppConfig
import me.anasmusa.learncast.lib.core.ApplicationLoader

class ApplicationLoader : ApplicationLoader() {
    override fun onCreate() {
        AppConfig.update(
            appName = "LearnCast",
            mainLogo = R.drawable.logo,
            transparentLogo = R.drawable.logo_transparent,
            apiBaseUrl = "https://api.anasmusa.me/learncast/",
            publicBaseUrl = "https://learncast.anasmusa.me",
            telegramBotId = 8538344134L,
            googleClientId = "22454749576-42ii04497d5aceqndkbvpnvn29nvub02.apps.googleusercontent.com",
        )
        super.onCreate()
    }
}
