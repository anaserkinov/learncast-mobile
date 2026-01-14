package me.anasmusa.learncast

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowInsetsControllerCompat
import me.anasmusa.learncast.data.worker.SyncWorker

abstract class Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        WindowInsetsControllerCompat(window, window.decorView)
            .let {
                it.isAppearanceLightStatusBars = false
                it.isAppearanceLightNavigationBars = false
            }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window?.isNavigationBarContrastEnforced = false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    "sync-worker",
                    "Background synchronization",
                    NotificationManager.IMPORTANCE_LOW,
                ).also {
                    it.description = "Keeps your data in sync across devices"
                },
            )
        }

        SyncWorker.enqueue(this)
    }
}
