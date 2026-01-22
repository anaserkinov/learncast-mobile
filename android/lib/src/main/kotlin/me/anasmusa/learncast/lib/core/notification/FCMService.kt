package me.anasmusa.learncast.lib.core.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import coil3.ImageLoader
import coil3.executeBlocking
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import me.anasmusa.learncast.core.appConfig

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val intent =
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("learncast://notification"),
                )

            val pendingIntent =
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

            val bitmap =
                it.imageUrl?.let { uri ->
                    val loader = ImageLoader(this)
                    val req =
                        ImageRequest
                            .Builder(this)
                            .data(uri)
                            .size(112)
                            .allowHardware(false)
                            .build()
                    loader
                        .executeBlocking(req)
                        .image
                        ?.toBitmap()
                }

            val notification =
                NotificationCompat
                    .Builder(this, it.channelId ?: "news")
                    .setSmallIcon(appConfig.transparentLogoInt)
                    .setContentTitle(it.title)
                    .setContentText(it.body)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .apply {
                        if (bitmap != null) {
                            setStyle(
                                NotificationCompat
                                    .BigPictureStyle()
                                    .bigPicture(bitmap),
                            )
                        }
                    }.build()

            manager.notify(System.currentTimeMillis().toInt(), notification)
        }
    }
}
