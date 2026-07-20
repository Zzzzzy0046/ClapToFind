package com.claptofind.phone.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.claptofind.phone.ClapToFindApp
import com.claptofind.phone.R
import com.claptofind.phone.data.SoundSensitivity
import com.claptofind.phone.ui.screens.StopAlertActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Foreground service that continuously listens for claps/whistles
 * in the background and triggers alerts when detected.
 */
class DetectionService : LifecycleService() {

    private lateinit var detector: ClapDetector
    private lateinit var prefsManager: com.claptofind.phone.data.PreferencesManager
    private lateinit var soundEngine: com.claptofind.phone.audio.SoundEngine
    private var wakeLock: PowerManager.WakeLock? = null
    private var isAlerting = false

    // Control notification action receiver
    private val controlReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_TOGGLE_DETECTION -> {
                    lifecycleScope.launch {
                        val enabled = !prefsManager.isDetectionEnabled.first()
                        prefsManager.setDetectionEnabled(enabled)
                        if (enabled) {
                            restartDetection()
                        } else {
                            stopDetection()
                        }
                        updateNotification()
                    }
                }
                ACTION_STOP_ALERT -> {
                    stopAlert()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as ClapToFindApp
        detector = ClapDetector(this)
        prefsManager = app.prefsManager
        soundEngine = app.soundEngine

        val filter = IntentFilter().apply {
            addAction(ACTION_TOGGLE_DETECTION)
            addAction(ACTION_STOP_ALERT)
        }
        registerReceiver(controlReceiver, filter, RECEIVER_EXPORTED)

        startForeground(
            ClapToFindApp.NOTIFICATION_SERVICE_ID,
            createServiceNotification()
        )

        // Observe detection events
        lifecycleScope.launch {
            detector.detectionEvents.collect { event ->
                if (event != null && !isAlerting) {
                    triggerAlert()
                    detector.resetDetectionEvent()
                }
            }
        }

        // Observe state changes
        lifecycleScope.launch {
            launch {
                prefsManager.isDetectionEnabled.collect { enabled ->
                    if (!enabled) stopDetection()
                    else {
                        val scheduleOn = prefsManager.scheduleEnabled.first()
                        if (!scheduleOn) startDetection()
                    }
                    updateNotification()
                }
            }
            launch {
                prefsManager.scheduleEnabled.collect {
                    val enabled = prefsManager.isDetectionEnabled.first()
                    if (!enabled) return@collect
                    val scheduleOn = prefsManager.scheduleEnabled.first()
                    val startH = prefsManager.scheduleStartHour.first()
                    val startM = prefsManager.scheduleStartMinute.first()
                    val endH = prefsManager.scheduleEndHour.first()
                    val endM = prefsManager.scheduleEndMinute.first()
                    if (isInSchedulePeriod(scheduleOn, startH, startM, endH, endM)) {
                        stopDetection()
                    } else {
                        startDetection()
                    }
                    updateNotification()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_ALERT) {
            stopAlert()
        }
        // Check if we should show alert screen
        if (intent?.getBooleanExtra(EXTRA_STOP_ALERT_SCREEN, false) == true) {
            stopAlert()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    override fun onDestroy() {
        stopDetection()
        stopAlert()
        unregisterReceiver(controlReceiver)
        super.onDestroy()
    }

    private fun startDetection() {
        if (detector.isActive()) return

        lifecycleScope.launch {
            val sensitivityName = prefsManager.soundSensitivity.first()
            val sensitivity = SoundSensitivity.fromDisplayName(sensitivityName)
            val whistleEnabled = prefsManager.whistleDetectionEnabled.first()

            // First-time use: max sensitivity
            val hasEverDetected = prefsManager.hasEverDetected.first()
            val effectiveSensitivity = if (!hasEverDetected) {
                SoundSensitivity.VERY_HIGH
            } else {
                sensitivity
            }

            detector.startListening(
                sensitivity = effectiveSensitivity,
                enableWhistle = whistleEnabled
            )
        }
    }

    private fun stopDetection() {
        detector.stopListening()
    }

    private fun restartDetection() {
        stopDetection()
        startDetection()
    }

    /**
     * Trigger full alert: sound + vibration + flashlight + screen wake
     */
    private fun triggerAlert() {
        if (isAlerting) return
        isAlerting = true

        // Mark as ever detected
        lifecycleScope.launch {
            prefsManager.setHasEverDetected(true)
        }

        // Acquire wake lock to turn on screen
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
            "ClapToFind:Alert"
        ).apply {
            acquire(3 * 60 * 1000L) // max 3 min
        }

        // Start the stop alert activity (full-screen overlay)
        val intent = Intent(this, StopAlertActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        startActivity(intent)

        // Play sound + vibration + flashlight
        lifecycleScope.launch {
            val volume = prefsManager.soundVolume.first()
            val duration = prefsManager.soundDuration.first()
            val soundName = prefsManager.selectedSound.first()
            val flashMode = prefsManager.flashlightMode.first()
            val vibrateMode = prefsManager.vibrateMode.first()
            val flashEnabled = prefsManager.flashlightEnabled.first()
            val vibrateEnabled = prefsManager.vibrateEnabled.first()

            soundEngine.playAlert(
                volume = volume,
                durationSeconds = duration,
                soundName = soundName,
                flashMode = flashMode,
                vibrateMode = vibrateMode,
                flashlightEnabled = flashEnabled,
                vibrateEnabled = vibrateEnabled
            )
        }

        // Show alert notification
        showAlertNotification()
    }

    fun stopAlert() {
        if (!isAlerting) return
        isAlerting = false

        soundEngine.stopAlert()
        wakeLock?.let {
            if (it.isHeld) it.release()
            wakeLock = null
        }

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ClapToFindApp.NOTIFICATION_ALERT_ID)

        // Send broadcast to close StopAlertActivity
        sendBroadcast(Intent(ACTION_ALERT_STOPPED).apply {
            setPackage(packageName)
        })
    }

    // --- Notification ---

    private fun createServiceNotification(): Notification {
        val toggleIntent = Intent(this, DetectionService::class.java).apply {
            action = ACTION_TOGGLE_DETECTION
        }
        val togglePending = PendingIntent.getService(
            this, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ClapToFindApp.CHANNEL_SERVICE)
            .setContentTitle("Clap to Find Phone")
            .setContentText("Listening for claps...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Stop", togglePending)
            .build()
    }

    private suspend fun updateNotification() {
        val isEnabled = prefsManager.isDetectionEnabled.first()
        val scheduleEnabled = prefsManager.scheduleEnabled.first()
        val startH = prefsManager.scheduleStartHour.first()
        val startM = prefsManager.scheduleStartMinute.first()
        val endH = prefsManager.scheduleEndHour.first()
        val endM = prefsManager.scheduleEndMinute.first()

        val isPaused = isInSchedulePeriod(scheduleEnabled, startH, startM, endH, endM)
        val title = "Clap to Find Phone"
        val text = when {
            !isEnabled -> "Detection disabled"
            isPaused -> "Temporarily paused until ${String.format("%02d:%02d", endH, endM)}"
            else -> "Listening for claps..."
        }

        val toggleIntent = Intent(this, DetectionService::class.java).apply {
            action = ACTION_TOGGLE_DETECTION
        }
        val togglePending = PendingIntent.getService(
            this, 0, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ClapToFindApp.CHANNEL_SERVICE)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(isEnabled)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                if (isEnabled) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isEnabled) "Stop" else "Start",
                togglePending
            )
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ClapToFindApp.NOTIFICATION_SERVICE_ID, notification)
    }

    private fun showAlertNotification() {
        val stopIntent = Intent(this, DetectionService::class.java).apply {
            action = ACTION_STOP_ALERT
        }
        val stopPending = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, ClapToFindApp.CHANNEL_ALERT)
            .setContentTitle("Phone Found!")
            .setContentText("Tap to stop the alert")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(stopPending, true)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPending)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ClapToFindApp.NOTIFICATION_ALERT_ID, notification)
    }

    /**
     * Check if current time falls within the scheduled deactivation period.
     */
    private fun isInSchedulePeriod(
        enabled: Boolean,
        startH: Int, startM: Int,
        endH: Int, endM: Int
    ): Boolean {
        if (!enabled) return false

        val calendar = java.util.Calendar.getInstance()
        val currentMinutes = calendar.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                calendar.get(java.util.Calendar.MINUTE)
        val startMinutes = startH * 60 + startM
        val endMinutes = endH * 60 + endM

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            // Overnight schedule (e.g., 22:00 - 06:00)
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }

    companion object {
        const val ACTION_TOGGLE_DETECTION = "com.claptofind.phone.ACTION_TOGGLE_DETECTION"
        const val ACTION_STOP_ALERT = "com.claptofind.phone.ACTION_STOP_ALERT"
        const val ACTION_ALERT_STOPPED = "com.claptofind.phone.ACTION_ALERT_STOPPED"
        const val EXTRA_STOP_ALERT_SCREEN = "stop_alert_screen"

        fun start(context: Context) {
            val intent = Intent(context, DetectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, DetectionService::class.java))
        }

        fun stopAlert(context: Context) {
            val intent = Intent(context, DetectionService::class.java).apply {
                action = ACTION_STOP_ALERT
            }
            context.startService(intent)
        }
    }
}
