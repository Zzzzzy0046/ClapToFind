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
        private const val BUFFER_SIZE = 31200
        private const val MIN_SCORE = 0.25f
    }

    private var classifier: AudioClassifier? = null
    private var recorder: AudioRecord? = null
    private var audioData: AudioData? = null
    @Volatile private var isRunning = false
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

        try {
            classifier = AudioClassifier.createFromOptions(
                context,
                AudioClassifier.AudioClassifierOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("yamnet.tflite").build())
                    .setRunningMode(RunningMode.AUDIO_CLIPS)
                    .setMaxResults(5)
                    .setScoreThreshold(MIN_SCORE)
                    .build()
            )

            // MediaPipe createAudioRecord(channels, sampleRate, bufferSize)
            recorder = try {
                classifier!!.createAudioRecord(1, SAMPLE_RATE, BUFFER_SIZE)
            } catch (_: Exception) {
                Log.w(TAG, "createAudioRecord failed, manual fallback")
                AudioRecord(
                    MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_FLOAT, BUFFER_SIZE
                )
            }

            audioData = AudioData.create(
                AudioData.AudioDataFormat.builder()
                    .setNumOfChannels(1)
                    .setSampleRate(SAMPLE_RATE.toFloat())
                    .build(),
                SAMPLE_RATE
            )

            recorder!!.startRecording()
            isRunning = true
            recentClaps.clear()

            job = CoroutineScope(Dispatchers.IO).launch {
                while (isActive && isRunning) {
                    val rec = recorder ?: break
                    val ad = audioData ?: break
                    val clf = classifier ?: break

                    if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                        try {
                            ad.load(rec)
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
                                            label.equals("Clapping", ignoreCase = true) && score > MIN_SCORE -> {
                                                recentClaps.add(now)
                                                while (recentClaps.isNotEmpty() &&
                                                    now - recentClaps.first() > CLAP_WINDOW_MS
                                                ) recentClaps.removeFirst()
                                                if (recentClaps.size >= 2) {
                                                    _detectionEvents.value = DetectionEvent(DetectionType.CLAP, score, label)
                                                    recentClaps.clear()
                                                }
                                            }
                                            label.equals("Whistling", ignoreCase = true) && enableWhistle && score > MIN_SCORE -> {
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

            Log.d(TAG, "YAMNet detector started (AUDIO_CLIPS, ${POLL_MS}ms)")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start: ${e.message}", e)
            return false
        }
    }

    fun stopListening() {
        isRunning = false
        job?.cancel(); job = null
        try { recorder?.stop(); recorder?.release() } catch (_: Exception) {}
        recorder = null
        audioData = null
        try { classifier?.close() } catch (_: Exception) {}
        classifier = null
        recentClaps.clear()
    }

    fun resetDetectionEvent() { _detectionEvents.value = null }
    fun isActive(): Boolean = isRunning
}
