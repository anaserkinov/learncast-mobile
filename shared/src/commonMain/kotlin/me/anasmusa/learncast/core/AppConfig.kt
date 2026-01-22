package me.anasmusa.learncast.core

var appConfig = AppConfig()
    private set

data class AppConfig(
    val appName: String = "",
    private val mainLogo: Any? = null,
    private val transparentLogo: Any? = null,
    val apiBaseUrl: String = "",
    val publicBaseUrl: String = "",
    val telegramBotId: Long = 0L,
    val googleClientId: String = "",
    val downloadNotificationTitle: Int = 0,
    val downloadNotificationMessage: Int = 0,
) {
    val mainLogoInt: Int
        get() = mainLogo as Int

    val transparentLogoInt: Int
        get() = transparentLogo as Int

    val mainLogoString: String
        get() = mainLogo as String

    val transparentLogoString: String
        get() = transparentLogo as String

    companion object {
        fun update(
            appName: String,
            mainLogo: Int,
            transparentLogo: Int,
            apiBaseUrl: String,
            publicBaseUrl: String,
            telegramBotId: Long,
            googleClientId: String,
        ) {
            appConfig =
                appConfig.copy(
                    appName = appName,
                    mainLogo = mainLogo,
                    transparentLogo = transparentLogo,
                    apiBaseUrl = apiBaseUrl,
                    publicBaseUrl = publicBaseUrl,
                    telegramBotId = telegramBotId,
                    googleClientId = googleClientId,
                )
        }

        fun update(
            appName: String,
            mainLogo: String,
            transparentLogo: String,
            apiBaseUrl: String,
            publicBaseUrl: String,
            telegramBotId: Long,
            googleClientId: String,
        ) {
            appConfig =
                appConfig.copy(
                    appName = appName,
                    mainLogo = mainLogo,
                    transparentLogo = transparentLogo,
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
