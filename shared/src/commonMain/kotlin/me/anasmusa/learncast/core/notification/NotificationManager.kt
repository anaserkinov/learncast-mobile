package me.anasmusa.learncast.core.notification

internal interface NotificationManager {
    fun subscribe()

    fun unSubscribe()
}

internal expect fun createNotificationManager(): NotificationManager
