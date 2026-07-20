package com.claptofind.phone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.service.DetectionService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Restarts detection service after device reboot if it was enabled before shutdown.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as ClapToFindApp
        runBlocking {
            if (app.prefsManager.isDetectionEnabled.first()) {
                DetectionService.start(context)
            }
        }
    }
}
