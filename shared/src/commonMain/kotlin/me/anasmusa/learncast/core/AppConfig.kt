package me.anasmusa.learncast.core

var appConfig = AppConfig()
    private set

data class AppConfig(
    val appName: String = "",
    val mainLogo: Int = 0,
    val loginLogo: Int = 0,
    val baseUrl: String = "",
    val telegramBotId: Long = 0L,
    val telegramOrigin: String = "",
    val googleClientId: String = "",
    val downloadNotificationTitle: Int = 0,
    val downloadNotificationMessage: Int = 0,
) {
    companion object {
        fun update(
            appName: String,
            mainLogo: Int,
            loginLogo: Int,
            baseUrl: String,
            telegramBotId: Long,
            telegramOrigin: String,
            googleClientId: String,
        ) {
            appConfig =
                appConfig.copy(
                    appName = appName,
                    mainLogo = mainLogo,
                    loginLogo = loginLogo,
                    baseUrl = baseUrl,
                    telegramBotId = telegramBotId,
                    telegramOrigin = telegramOrigin,
                    googleClientId = googleClientId,
                )
        }

        fun update(
            downloadNotificationTitle: Int = 0,
            downloadNotificationMessage: Int = 0,
        ) {
            appConfig =
                appConfig.copy(
                    downloadNotificationTitle = downloadNotificationTitle,
                    downloadNotificationMessage = downloadNotificationMessage,
                )
        }
    }
}
