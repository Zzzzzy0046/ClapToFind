package com.claptofind.phone.service

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.claptofind.phone.ClapToFindApp
import kotlinx.coroutines.flow.first

/**
 * Worker that periodically checks whether the current time falls within
 * the user's scheduled deactivation window. If so, it stops detection;
 * otherwise, it ensures detection is running (if enabled).
 */
class ScheduleCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as ClapToFindApp
        val prefs = app.prefsManager

        val isEnabled = prefs.isDetectionEnabled.first()
        val scheduleOn = prefs.scheduleEnabled.first()

        if (!isEnabled || !scheduleOn) {
            Log.d("ScheduleCheckWorker", "Detection or schedule disabled, nothing to do")
            return Result.success()
        }

        val startH = prefs.scheduleStartHour.first()
        val startM = prefs.scheduleStartMinute.first()
        val endH = prefs.scheduleEndHour.first()
        val endM = prefs.scheduleEndMinute.first()

        val calendar = java.util.Calendar.getInstance()
        val currentMin = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                calendar.get(java.util.Calendar.MINUTE)
        val startMin = startH * 60 + startM
        val endMin = endH * 60 + endM

        val inSchedule = if (startMin <= endMin) {
            currentMin in startMin..endMin
        } else {
            currentMin >= startMin || currentMin <= endMin
        }

        if (inSchedule) {
            Log.d("ScheduleCheckWorker", "In schedule window, stopping detection")
            DetectionService.stop(applicationContext)
        } else {
            Log.d("ScheduleCheckWorker", "Outside schedule window, starting detection")
            DetectionService.start(applicationContext)
        }

        return Result.success()
    }
}
