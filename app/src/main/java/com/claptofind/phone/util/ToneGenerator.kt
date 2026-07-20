package com.claptofind.phone.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.*

/**
 * Generates synthetic sound tones for the 24 sound effects.
 * In production, these would be real audio files in res/raw/.
 * This provides a complete, working audio experience without external assets.
 */
class ToneGenerator(private val context: Context) {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    @Volatile private var isPlaying = false

    /**
     * Generate and play a synthetic tone for the given sound name.
     */
    fun playTone(soundName: String, volumePercent: Int, durationMs: Int) {
        stop()

        val volume = (volumePercent.coerceIn(0, 200) / 200f).coerceIn(0f, 1f)
        val samples = generateSamples(soundName, durationMs)

        isPlaying = true

        Thread {
            try {
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .build()

                audioTrack?.play()

                // Write samples in chunks
                val chunkSize = bufferSize / 2
                var offset = 0
                while (offset < samples.size && isPlaying) {
                    val end = minOf(offset + chunkSize, samples.size)
                    audioTrack?.write(samples, offset, end - offset)
                    offset = end
                }

                audioTrack?.stop()
                audioTrack?.release()
                audioTrack = null
            } catch (_: Exception) {}
        }.start()
    }

    fun stop() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    private fun generateSamples(soundName: String, durationMs: Int): ShortArray {
        val numSamples = (sampleRate * durationMs / 1000)
        val samples = ShortArray(numSamples)

        when (soundName) {
            "Air Horn" -> generateAirHorn(samples)
            "Siren" -> generateSiren(samples)
            "Police Siren" -> generatePoliceSiren(samples)
            "Guitar Strum" -> generateGuitarStrum(samples)
            "Duck Quack" -> generateDuckQuack(samples)
            "Piano Tone" -> generatePianoTone(samples)
            "Cow Moo" -> generateCowMoo(samples)
            "Cartoon Boom" -> generateCartoonBoom(samples)
            "Bird Chirp" -> generateBirdChirp(samples)
            "Drum Beat" -> generateDrumBeat(samples)
            "Cat Meow" -> generateCatMeow(samples)
            "Doorbell Chime" -> generateDoorbellChime(samples)
            "Wood Block" -> generateWoodBlock(samples)
            "Horse Neigh" -> generateHorseNeigh(samples)
            "Game Coin / Pop" -> generateGameCoin(samples)
            "Dog Bark" -> generateDogBark(samples)
            "Elephant" -> generateElephant(samples)
            "Violin Snip" -> generateViolinSnip(samples)
            "Radar Ping" -> generateRadarPing(samples)
            "Metronome Tick" -> generateMetronomeTick(samples)
            "Hi-hat" -> generateHiHat(samples)
            "White Noise" -> generateWhiteNoise(samples)
            "Rooster Crow" -> generateRoosterCrow(samples)
            "Cricket Chirp" -> generateCricketChirp(samples)
            else -> generateAirHorn(samples)
        }

        return samples
    }

    // --- Sound generation functions ---

    private fun generateAirHorn(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 440.0 + 100.0 * Math.sin(2.0 * Math.PI * 8.0 * t)
            val amp = 0.8 * (1.0 - t / (samples.size.toDouble() / sampleRate)).coerceAtLeast(0.0)
            samples[i] = (amp * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateSiren(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val period = 1.0
            val phase = (t % period) / period
            val freq = 600.0 + 800.0 * phase
            samples[i] = (0.7 * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generatePoliceSiren(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val period = 0.8
            val phase = (t % period) / period
            val freq = if (phase < 0.5) 800.0 else 600.0
            samples[i] = (0.7 * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateDuckQuack(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 800.0 + 200.0 * Math.sin(2.0 * Math.PI * 50.0 * t)
            val env = if (t < 0.1) 1.0 else Math.exp(-8.0 * (t - 0.1))
            samples[i] = (0.5 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generatePianoTone(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-4.0 * t)
            // C major chord: C4 (262Hz) + E4 (330Hz) + G4 (392Hz)
            val note = Math.sin(2.0 * Math.PI * 262.0 * t) +
                    Math.sin(2.0 * Math.PI * 330.0 * t) * 0.7 +
                    Math.sin(2.0 * Math.PI * 392.0 * t) * 0.6
            samples[i] = (0.5 * env * Short.MAX_VALUE * note / 2.3).toInt().toShort()
        }
    }

    private fun generateGuitarStrum(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-6.0 * t)
            val chord = Math.sin(2.0 * Math.PI * 330.0 * t) +
                    Math.sin(2.0 * Math.PI * 392.0 * t) * 0.8 +
                    Math.sin(2.0 * Math.PI * 523.0 * t) * 0.6 +
                    Math.sin(2.0 * Math.PI * 659.0 * t) * 0.5
            samples[i] = (0.4 * env * Short.MAX_VALUE * chord / 2.9).toInt().toShort()
        }
    }

    private fun generateCowMoo(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 180.0 + 60.0 * Math.sin(2.0 * Math.PI * 3.0 * t)
            val env = Math.sin(Math.PI * t / 1.5).coerceAtLeast(0.0)
            samples[i] = (0.4 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateCartoonBoom(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 100.0 * Math.exp(-6.0 * t) + 30.0
            val env = Math.exp(-3.0 * t)
            samples[i] = (0.8 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateBirdChirp(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 2000.0 + 1500.0 * Math.sin(2.0 * Math.PI * 15.0 * t)
            val env = Math.exp(-5.0 * t)
            samples[i] = (0.3 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateDrumBeat(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 180.0 * Math.exp(-10.0 * t) + 60.0
            val env = Math.exp(-8.0 * t)
            val noise = (Math.random() - 0.5) * Math.exp(-20.0 * t)
            val tone = Math.sin(2.0 * Math.PI * freq * t)
            samples[i] = (0.7 * env * Short.MAX_VALUE * (tone * 0.5 + noise)).toInt().toShort()
        }
    }

    private fun generateCatMeow(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freqUp = 500.0 + 300.0 * (t / 0.5).coerceAtMost(1.0)
            val freqDown = freqUp - 200.0 * ((t - 0.3) / 0.3).coerceIn(0.0, 1.0)
            val freq = if (t < 0.3) freqUp else freqDown
            val env = Math.sin(Math.PI * t / 0.8).coerceAtLeast(0.0)
            samples[i] = (0.4 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateDoorbellChime(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-3.0 * t)
            // Ding-dong: 740Hz then 590Hz
            val freq = if (t < 0.3) 740.0 else 590.0
            val env2 = if (t < 0.3) 1.0 else Math.exp(-3.0 * (t - 0.3))
            samples[i] = (0.5 * env * env2 * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateWoodBlock(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 1200.0
            val env = Math.exp(-15.0 * t)
            samples[i] = (0.6 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateHorseNeigh(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 600.0 + 400.0 * Math.sin(2.0 * Math.PI * 6.0 * t)
            val env = Math.exp(-3.0 * t)
            samples[i] = (0.35 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateGameCoin(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = if (t < 0.05) 1.0 else Math.exp(-12.0 * (t - 0.05))
            val note1 = Math.sin(2.0 * Math.PI * 988.0 * t) // B5
            val note2 = Math.sin(2.0 * Math.PI * 1319.0 * t) * 0.6 // E6
            samples[i] = (0.5 * env * Short.MAX_VALUE * (note1 + note2) / 1.6).toInt().toShort()
        }
    }

    private fun generateDogBark(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 300.0 + 200.0 * Math.sin(2.0 * Math.PI * 30.0 * t)
            val env = Math.exp(-6.0 * t)
            samples[i] = (0.5 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateElephant(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 50.0 + 30.0 * Math.sin(2.0 * Math.PI * 2.0 * t)
            val env = Math.sin(Math.PI * t / 2.0).coerceAtLeast(0.0)
            val vibrato = Math.sin(2.0 * Math.PI * 8.0 * t + 5.0 * Math.sin(2.0 * Math.PI * 2.0 * t))
            samples[i] = (0.5 * env * Short.MAX_VALUE * vibrato).toInt().toShort()
        }
    }

    private fun generateViolinSnip(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 440.0 // A4
            val env = Math.exp(-3.0 * t)
            val vibrato = Math.sin(2.0 * Math.PI * freq * t + 0.01 * Math.sin(2.0 * Math.PI * 6.0 * t))
            samples[i] = (0.4 * env * Short.MAX_VALUE * vibrato).toInt().toShort()
        }
    }

    private fun generateRadarPing(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 3000.0 + 2000.0 * (1.0 - t / (samples.size.toDouble() / sampleRate))
            val env = Math.sin(Math.PI * t / 0.15).coerceAtLeast(0.0)
            samples[i] = (0.4 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateMetronomeTick(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-30.0 * t)
            samples[i] = (0.8 * env * Short.MAX_VALUE * (Math.random() - 0.5)).toInt().toShort()
        }
    }

    private fun generateHiHat(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val env = Math.exp(-20.0 * t)
            samples[i] = (0.3 * env * Short.MAX_VALUE * (Math.random() - 0.5)).toInt().toShort()
        }
    }

    private fun generateWhiteNoise(samples: ShortArray) {
        for (i in samples.indices) {
            samples[i] = (0.3 * Short.MAX_VALUE * (Math.random() - 0.5)).toInt().toShort()
        }
    }

    private fun generateRoosterCrow(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 800.0 + 300.0 * Math.sin(2.0 * Math.PI * 10.0 * t)
            val env = Math.sin(Math.PI * t / 0.6).coerceAtLeast(0.0) * Math.exp(-2.0 * t)
            samples[i] = (0.3 * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }

    private fun generateCricketChirp(samples: ShortArray) {
        for (i in samples.indices) {
            val t = i.toDouble() / sampleRate
            val freq = 4500.0
            val pulse = if ((t * 30).toInt() % 5 < 2) 1.0 else 0.0
            val env = Math.exp(-2.0 * t)
            samples[i] = (0.2 * pulse * env * Short.MAX_VALUE * Math.sin(2.0 * Math.PI * freq * t)).toInt().toShort()
        }
    }
}
