package me.anasmusa.learncast.core.notification

private class IosNotificationManager : NotificationManager {
    override fun subscribe() {
    }

    override fun unSubscribe() {
    }
}

internal actual fun createNotificationManager(): NotificationManager = IosNotificationManager()
