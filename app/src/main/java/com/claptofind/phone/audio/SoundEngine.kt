package com.claptofind.phone.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.claptofind.phone.data.FlashlightMode
import com.claptofind.phone.data.VibrateMode
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class SoundEngine(private val context: Context) {

    private val soundPool: SoundPool
    private val vibrator: Vibrator

    private var isPlaying = false
    private var flashlightJob: Job? = null
    private var vibrateJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(audioAttributes)
            .build()

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun playAlert(
        volume: Int,
        durationSeconds: Int,
        soundName: String,
        flashMode: String,
        vibrateMode: String,
        flashlightEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        stopAlert()

        if (vibrateEnabled) {
            vibrateJob = scope.launch { startVibration(VibrateMode.fromDisplayName(vibrateMode), durationSeconds) }
        }
        if (flashlightEnabled) {
            flashlightJob = scope.launch { startFlashlight(FlashlightMode.fromDisplayName(flashMode), durationSeconds) }
        }

        // Sound disabled for now — only vibrate + flashlight
        isPlaying = true
    }

    fun playPreview(soundName: String, volume: Int) {
        stopAlert()
        isPlaying = true
        scope.launch { playToneLoop(5) }
    }

    fun playFlashlightPreview(mode: FlashlightMode) {
        flashlightJob?.cancel()
        flashlightJob = scope.launch { startFlashlight(mode, 2) }
    }

    fun playVibratePreview(mode: VibrateMode) {
        vibrateJob?.cancel()
        vibrateJob = scope.launch { startVibration(mode, 2) }
    }

    fun stopAlert() {
        isPlaying = false
        soundPool.autoPause()
        flashlightJob?.cancel()
        vibrateJob?.cancel()
        vibrator.cancel()
    }

    fun release() {
        stopAlert()
        soundPool.release()
        scope.cancel()
    }

    private suspend fun playToneLoop(durationSeconds: Int) {
        val toneGenerator = android.media.ToneGenerator(AudioManager.STREAM_ALARM, 100)
        val endTime = System.currentTimeMillis() + durationSeconds * 1000L
        while (isPlaying && System.currentTimeMillis() < endTime) {
            toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
            delay(200)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_AUTOREDIAL_LITE, 200)
            delay(300)
            if (isPlaying && System.currentTimeMillis() < endTime) {
                toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200)
                delay(200)
                toneGenerator.startTone(android.media.ToneGenerator.TONE_CDMA_PIP, 150)
                delay(400)
            }
        }
        toneGenerator.release()
    }

    private suspend fun startVibration(mode: VibrateMode, durationSeconds: Int) {
        if (!vibrator.hasVibrator()) return
        val endTime = System.currentTimeMillis() + durationSeconds * 1000L
        while (isActive() && System.currentTimeMillis() < endTime) {
            val pattern = getVibrationPattern(mode)
            val vibe = VibrationEffect.createWaveform(pattern.toLongArray(), -1)
            vibrator.vibrate(vibe)
            delay(pattern.sum())
        }
    }

    private fun getVibrationPattern(mode: VibrateMode): List<Long> = when (mode) {
        VibrateMode.HIGH_FREQUENCY -> listOf(0L, 100, 100, 100, 100, 100, 100, 100)
        VibrateMode.MEDIUM_FREQUENCY -> listOf(0L, 300, 300, 300, 300, 300)
        VibrateMode.SLOW_FREQUENCY -> listOf(0L, 600, 600, 600, 600)
        VibrateMode.SOS_PATTERN -> listOf(
            0L, 200, 200, 200, 200, 200, 200, 200,
            200, 600, 200, 600, 200, 600,
            200, 200, 200, 200, 200, 200
        )
        VibrateMode.RHYTHMIC_PATTERN -> listOf(
            0L, 200, 100, 200, 500, 200, 100, 200, 500, 200, 100, 200
        )
        VibrateMode.RANDOM_PATTERN -> {
            val count = 10
            (0 until count).flatMap {
                listOf((100..700).random().toLong(), (50..300).random().toLong())
            }
        }
    }

    private suspend fun startFlashlight(mode: FlashlightMode, durationSeconds: Int) {
        val flashlight = FlashlightController(context)
        if (!flashlight.isAvailable()) return
        val endTime = System.currentTimeMillis() + durationSeconds * 1000L
        try {
            while (isActive() && System.currentTimeMillis() < endTime) {
                val pattern = getFlashlightPattern(mode)
                for ((index, state) in pattern.withIndex()) {
                    if (!isActive() || System.currentTimeMillis() >= endTime) break
                    if (state) flashlight.turnOn() else flashlight.turnOff()
                    delay(100L)
                }
            }
        } finally {
            flashlight.turnOff()
            flashlight.release()
        }
    }

    private fun getFlashlightPattern(mode: FlashlightMode): BooleanArray = when (mode) {
        FlashlightMode.QUICK_BLINK -> booleanArrayOf(true, false, true, false, true, false)
        FlashlightMode.MEDIUM_BLINK -> booleanArrayOf(true, true, true, false, false, false)
        FlashlightMode.SLOW_BLINK -> booleanArrayOf(true, true, true, true, true, true, false, false, false, false, false, false)
        FlashlightMode.SOS_BLINK -> booleanArrayOf(
            true, false, true, false, true,
            false, false,
            true, true, true, false, true, true, true, false, true, true, true,
            false, false,
            true, false, true, false, true
        )
        FlashlightMode.CONTINUOUS_BLINK -> BooleanArray(20) { true }
        FlashlightMode.RANDOM_BLINK -> BooleanArray(20) { (0..1).random() == 1 }
    }

    private fun isActive(): Boolean = isPlaying || flashlightJob?.isActive == true || vibrateJob?.isActive == true
}
