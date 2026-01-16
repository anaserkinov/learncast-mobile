package me.anasmusa.learncast.lib

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.clientVersionStalenessDays
import me.anasmusa.learncast.data.worker.SyncWorker

abstract class Activity : ComponentActivity() {
    private lateinit var updateResultLauncher: ActivityResultLauncher<IntentSenderRequest>

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
            notificationManager.createNotificationChannels(
                listOf(
                    NotificationChannel(
                        "news",
                        "News",
                        NotificationManager.IMPORTANCE_LOW,
                    ).also {
                        it.description = "General news and updates"
                    },
                    NotificationChannel(
                        "app-updates",
                        "App updates",
                        NotificationManager.IMPORTANCE_LOW,
                    ).also {
                        it.description = "App updates and improvements"
                    },
                ),
            )
        }

        SyncWorker.enqueue(this)

        updateResultLauncher =
            registerForActivityResult(
                ActivityResultContracts.StartIntentSenderForResult(),
            ) { result ->
            }

        val appUpdateManager = AppUpdateManagerFactory.create(this)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        // TODO: implement full update flow
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            appUpdateInfo.clientVersionStalenessDays()
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                val clientVersionStalenessDays = appUpdateInfo.clientVersionStalenessDays ?: -1
                if (clientVersionStalenessDays > 7) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                        requestImmediateUpdate(appUpdateManager, appUpdateInfo)
                    }
                } else if (clientVersionStalenessDays > 1) {
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {
                        requestImmediateUpdate(appUpdateManager, appUpdateInfo)
                    }
                }
            }
        }
    }

    private fun requestImmediateUpdate(
        appUpdateManager: AppUpdateManager,
        appUpdateInfo: AppUpdateInfo,
    ) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            updateResultLauncher,
            AppUpdateOptions
                .newBuilder(AppUpdateType.IMMEDIATE)
                .build(),
        )
    }
}
