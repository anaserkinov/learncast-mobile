package me.anasmusa.learncast.core.notification

import com.google.firebase.messaging.FirebaseMessaging

internal class AndroidNotificationManager : NotificationManager {
    override fun subscribe() {
        FirebaseMessaging
            .getInstance()
            .subscribeToTopic("news")
    }

    override fun unSubscribe() {
        FirebaseMessaging
            .getInstance()
            .unsubscribeFromTopic("news")
    }
}

internal actual fun createNotificationManager(): NotificationManager = AndroidNotificationManager()
