package com.claptofind.phone

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.claptofind.phone.data.PreferencesManager
import com.claptofind.phone.audio.SoundEngine

class ClapToFindApp : Application() {

    lateinit var prefsManager: PreferencesManager
        private set
    lateinit var soundEngine: SoundEngine
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefsManager = PreferencesManager(this)
        soundEngine = SoundEngine(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                CHANNEL_SERVICE,
                "Detection Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when clap detection is active"
                setShowBadge(false)
            }
            manager.createNotificationChannel(serviceChannel)

            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Phone Found Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when your phone is found"
            }
            manager.createNotificationChannel(alertChannel)
        }
    }

    companion object {
        lateinit var instance: ClapToFindApp
            private set

        const val CHANNEL_SERVICE = "detection_service"
        const val CHANNEL_ALERT = "phone_found_alert"
        const val NOTIFICATION_SERVICE_ID = 1001
        const val NOTIFICATION_ALERT_ID = 1002
    }
}
