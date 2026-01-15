package me.anasmusa.learncast.core

var appConfig = AppConfig()
    private set

data class AppConfig(
    val appName: String = "",
    val mainLogo: Int = 0,
    val transparentLogo: Int = 0,
    val apiBaseUrl: String = "",
    val publicBaseUrl: String = "",
    val telegramBotId: Long = 0L,
    val googleClientId: String = "",
    val downloadNotificationTitle: Int = 0,
    val downloadNotificationMessage: Int = 0,
) {
    companion object {
        fun update(
            appName: String,
            mainLogo: Int,
            loginLogo: Int,
            apiBaseUrl: String,
            publicBaseUrl: String,
            telegramBotId: Long,
            googleClientId: String,
        ) {
            appConfig =
                appConfig.copy(
                    appName = appName,
                    mainLogo = mainLogo,
                    transparentLogo = loginLogo,
                    apiBaseUrl = apiBaseUrl,
                    publicBaseUrl = publicBaseUrl,
                    telegramBotId = telegramBotId,
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
