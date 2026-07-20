package com.claptofind.phone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.service.DetectionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_CHECK_SCHEDULE) {
            val pendingResult = goAsync()
            val app = context.applicationContext as ClapToFindApp
            val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.IO)
            scope.launch {
                try {
                    checkAndApplySchedule(context, app)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private suspend fun checkAndApplySchedule(context: Context, app: ClapToFindApp) {
        val prefs = app.prefsManager

        val isEnabled = prefs.isDetectionEnabled.first()
        val scheduleOn = prefs.scheduleEnabled.first()
        if (!isEnabled) return

        val startH = prefs.scheduleStartHour.first()
        val startM = prefs.scheduleStartMinute.first()
        val endH = prefs.scheduleEndHour.first()
        val endM = prefs.scheduleEndMinute.first()

        val calendar = Calendar.getInstance()
        val currentMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        val startMin = startH * 60 + startM
        val endMin = endH * 60 + endM

        val inSchedule = if (startMin <= endMin) {
            currentMin in startMin..endMin
        } else {
            currentMin >= startMin || currentMin <= endMin
        }

        if (inSchedule && scheduleOn) {
            DetectionService.stop(context)
        } else if (!inSchedule && isEnabled) {
            DetectionService.start(context)
        }
    }

    companion object {
        const val ACTION_CHECK_SCHEDULE = "com.claptofind.phone.CHECK_SCHEDULE"
    }
}
