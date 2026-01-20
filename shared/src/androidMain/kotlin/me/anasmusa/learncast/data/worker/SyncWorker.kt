package me.anasmusa.learncast.data.worker

import android.app.Notification
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import me.anasmusa.learncast.Resource.string
import me.anasmusa.learncast.Strings
import me.anasmusa.learncast.data.repository.abstraction.SyncRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams),
    KoinComponent {
    companion object {
        fun enqueue(context: Context) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .setRequiresBatteryNotLow(true)
                    .build()

            val request =
                PeriodicWorkRequestBuilder<SyncWorker>(4, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build()

            WorkManager
                .getInstance(context)
                .enqueueUniquePeriodicWork(
                    "sync_worker",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request,
                )
        }
    }

    private val syncRepository by inject<SyncRepository>()

    override suspend fun doWork(): Result {
        syncRepository.sync(false)
        return Result.success()
    }

    override suspend fun getForegroundInfo(): ForegroundInfo =
        ForegroundInfo(
            1,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification
                    .Builder(context, "sync-worker")
                    .setContentTitle(Strings.SYNC_NOTIFICATION_TITLE.string())
                    .setContentText(Strings.SYNC_NOTIFICATION_MESSAGE.string())
                    .build()
            } else {
                Notification
                    .Builder(context)
                    .setContentTitle(Strings.SYNC_NOTIFICATION_TITLE.string())
                    .setContentText(Strings.SYNC_NOTIFICATION_MESSAGE.string())
                    .build()
            },
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )
}
