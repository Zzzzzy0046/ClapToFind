package com.claptofind.phone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.service.DetectionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

/**
 * Restarts detection service after device reboot if it was enabled before shutdown.
 * On Android 12+, foreground services cannot be started directly from BOOT_COMPLETED.
 * We enqueue a WorkManager expedited work request instead.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as ClapToFindApp
        val wasEnabled = runBlocking {
            app.prefsManager.isDetectionEnabled.first()
        }

        if (!wasEnabled) return

        Log.d("BootReceiver", "Device booted, scheduling detection restart via WorkManager")

        val workRequest = OneTimeWorkRequestBuilder<BootRestartWorker>()
            .setInitialDelay(30, TimeUnit.SECONDS)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

/**
 * Worker that starts the DetectionService after boot delay.
 * WorkManager workers are exempt from Android 12+ foreground service launch restrictions.
 */
class BootRestartWorker(
    context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val app = applicationContext as ClapToFindApp
        val isEnabled = runBlocking {
            app.prefsManager.isDetectionEnabled.first()
        }

        return if (isEnabled) {
            try {
                DetectionService.start(applicationContext)
                Log.d("BootRestartWorker", "Detection service restarted after boot")
                Result.success()
            } catch (e: Exception) {
                Log.e("BootRestartWorker", "Failed to restart detection: ${e.message}")
                Result.retry()
            }
        } else {
            Log.d("BootRestartWorker", "Detection was disabled, skipping restart")
            Result.success()
        }
    }
}
