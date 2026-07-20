package com.claptofind.phone.service

import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.google.mediapipe.tasks.audio.audioclassifier.AudioClassifier
import com.google.mediapipe.tasks.audio.core.RunningMode
import com.google.mediapipe.tasks.components.containers.AudioData
import com.google.mediapipe.tasks.core.BaseOptions
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Clap/whistle detector: MediaPipe AudioClassifier + YAMNet (AUDIO_CLIPS mode).
 *
 * API chain: AudioClassifierResult → classificationResults() → List<ClassificationResult>
 *   → each .classifications() → List<Classifications> → each .categories() → List<Category>
 *   → each .categoryName() / .score()
 *
 * Mirror of: findmyphone...utils.tflite.AudioClassifierHelper (reference APK).
 */
class ClapDetector(private val context: Context) {

    companion object {
        private const val TAG = "ClapDetector"
        private const val SAMPLE_RATE = 16000
        private const val POLL_MS = 500L
        private const val CLAP_WINDOW_MS = 1500L
        // Calculated from: 16000 samples/s * 4 bytes(float)/sample * 0.975s ≈ 62400; rounded for buffer alignment
        private val BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE,
            android.media.AudioFormat.CHANNEL_IN_MONO,
            android.media.AudioFormat.ENCODING_PCM_FLOAT
        ).let { if (it > 0) it else 31200 }

        /**
         * Map SoundSensitivity to YAMNet score threshold.
         * SoundSensitivity.dbThreshold represents the approximate dB level; lower dB = more sensitive.
         * We map: VERY_HIGH(45dB) → 0.15, HIGH(55dB) → 0.25, MEDIUM(65dB) → 0.40.
         */
        fun sensitivityToThreshold(sensitivity: com.claptofind.phone.data.SoundSensitivity): Float = when (sensitivity) {
            com.claptofind.phone.data.SoundSensitivity.VERY_HIGH -> 0.15f
            com.claptofind.phone.data.SoundSensitivity.HIGH -> 0.25f
            com.claptofind.phone.data.SoundSensitivity.MEDIUM -> 0.40f
        }
    }

    private var classifier: AudioClassifier? = null
    private var recorder: AudioRecord? = null
    private var audioData: AudioData? = null
    @Volatile private var isRunning = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null

    private val _detectionEvents = MutableStateFlow<DetectionEvent?>(null)
    val detectionEvents: StateFlow<DetectionEvent?> = _detectionEvents
    private val recentClaps = ArrayDeque<Long>(10)

    data class DetectionEvent(val type: DetectionType, val confidence: Float, val label: String = "")
    enum class DetectionType { CLAP, WHISTLE }

    fun startListening(
        sensitivity: com.claptofind.phone.data.SoundSensitivity = com.claptofind.phone.data.SoundSensitivity.HIGH,
        enableWhistle: Boolean = true
    ): Boolean {
        if (isRunning) return true

        val threshold = sensitivityToThreshold(sensitivity)

        try {
            classifier = AudioClassifier.createFromOptions(
                context,
                AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("yamnet.tflite").build())
                    .setRunningMode(RunningMode.AUDIO_CLIPS)
                    .setMaxResults(5)
                    .setScoreThreshold(threshold)
                    .build()
            )

            // MediaPipe createAudioRecord(channels, sampleRate, bufferSize)
            recorder = try {
                classifier!!.createAudioRecord(1, SAMPLE_RATE, BUFFER_SIZE)
            } catch (_: Exception) {
                Log.w(TAG, "createAudioRecord failed, manual fallback")
                try {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        android.media.AudioFormat.CHANNEL_IN_MONO,
                        android.media.AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Manual AudioRecord fallback also failed: ${e.message}")
                    classifier?.close()
                    classifier = null
                    return false
                }
            }

            val rec = recorder ?: run {
                classifier?.close()
                classifier = null
                Log.e(TAG, "Failed to create AudioRecord")
                return false
            }

            audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(SAMPLE_RATE.toFloat())
                    .build(),
                SAMPLE_RATE
            )

            rec.startRecording()
            isRunning = true
            recentClaps.clear()

            job = scope.launch {
                while (isActive && isRunning) {
                    val currentRec = recorder ?: break
                    val ad = audioData ?: break
                    val clf = classifier ?: break

                    if (currentRec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        try {
                            ad.load(currentRec)
                            val result = clf.classify(ad)

                            val crList = result.classificationResults()
                            if (crList.isNotEmpty()) {
                                val clList = crList[0].classifications()
                                if (clList.isNotEmpty()) {
                                    val cats = clList[0].categories()
                                    if (cats.isNotEmpty()) {
                                        val top = cats[0]
                                        val label = top.categoryName()
                                        val score = top.score()
                                        val now = System.currentTimeMillis()

                                        Log.d(TAG, "YAMNet: $label (${"%.3f".format(score)})")

                                        when {
                                            label.equals("Clapping", ignoreCase = true) && score > threshold -> {
                                                recentClaps.add(now)
                                                while (recentClaps.isNotEmpty() &&
                                                    now - recentClaps.first() > CLAP_WINDOW_MS
                                                ) recentClaps.removeFirst()
                                                if (recentClaps.size >= 2) {
                                                    _detectionEvents.value = DetectionEvent(DetectionType.CLAP, score, label)
                                                    recentClaps.clear()
                                                }
                                            }
                                            label.equals("Whistling", ignoreCase = true) && enableWhistle && score > threshold -> {
                                                _detectionEvents.value = DetectionEvent(DetectionType.WHISTLE, score, label)
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "classify error: ${e.message}")
                        }
                    }
                    delay(POLL_MS)
                }
            }

            Log.d(TAG, "YAMNet detector started (AUDIO_CLIPS, ${POLL_MS}ms, threshold=$threshold)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start: ${e.message}", e)
            try { classifier?.close() } catch (_: Exception) {}
            classifier = null
            return false
        }
    }

    fun stopListening() {
        isRunning = false
        scope.coroutineContext.cancelChildren()
        try { recorder?.stop(); recorder?.release() } catch (e: Exception) { Log.w(TAG, "Failed to release recorder: ${e.message}") }
        recorder = null
        audioData = null
        try { classifier?.close() } catch (e: Exception) { Log.w(TAG, "Failed to close classifier: ${e.message}") }
        classifier = null
        recentClaps.clear()
    }

    fun resetDetectionEvent() { _detectionEvents.value = null }
    fun isActive(): Boolean = isRunning
}
