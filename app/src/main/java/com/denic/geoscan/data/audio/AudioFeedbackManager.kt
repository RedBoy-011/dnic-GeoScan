package com.denic.geoscan.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

/**
 * Audio feedback operational modes for real-time telemetry.
 */
enum class TelemetryAudioMode(val code: String) {
    DYNAMIC_Z_PITCH("z_pitch"), // Sound pitch dynamically modulated by Z anomaly value
    SIMPLE_BEEP("simple"),      // Short clean acoustic chirp per point
    MUTE("mute");               // Completely silent

    companion object {
        fun fromCode(code: String): TelemetryAudioMode =
            entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DYNAMIC_Z_PITCH
    }
}

/**
 * High-performance, zero-latency Audio Telemetry Feedback and Voice Announcement Engine.
 *
 * Synthesizes pure PCM waveforms dynamically without external asset dependencies.
 * Includes Text-To-Speech (TTS) for hardware connection voice notifications.
 */
class AudioFeedbackManager(private val context: Context) {

    private val audioExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val sampleRate = 44100

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false

    init {
        initTts()
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                }
            }
        } catch (_: Exception) {
            isTtsInitialized = false
        }
    }

    /**
     * Plays acoustic feedback for an acquired data point according to selected [TelemetryAudioMode].
     */
    fun playPointFeedback(zValue: Float, mode: TelemetryAudioMode) {
        if (mode == TelemetryAudioMode.MUTE) return

        audioExecutor.execute {
            try {
                when (mode) {
                    TelemetryAudioMode.SIMPLE_BEEP -> {
                        playTone(frequency = 880f, durationMs = 45, volume = 0.5f)
                    }
                    TelemetryAudioMode.DYNAMIC_Z_PITCH -> {
                        val frequency = calculateFrequencyForZ(zValue)
                        playTone(frequency = frequency, durationMs = 60, volume = 0.6f)
                    }
                    TelemetryAudioMode.MUTE -> { /* no-op */ }
                }
            } catch (_: Exception) {
                // Audio subsystem resilience - never interrupt data telemetry
            }
        }
    }

    /**
     * Maps magnetometer reading Z (microTesla anomaly or raw reading) to a discernible acoustic frequency.
     *
     * - Positive anomalies (Metals/Ferrous): Sharp, high frequencies (700 Hz up to 2200 Hz)
     * - Negative anomalies (Cavities/Voids): Deep, low frequencies (180 Hz up to 450 Hz)
     * - Nominal background soil: Balanced mid-tone (550 Hz)
     */
    fun calculateFrequencyForZ(zValue: Float): Float {
        return when {
            zValue > 20f -> {
                // Metal: pitch rises proportionally with field intensity
                (600f + (zValue - 20f) * 2.2f).coerceIn(600f, 2200f)
            }
            zValue < -20f -> {
                // Cavity: pitch deepens into low rumble
                (500f + (zValue + 20f) * 1.5f).coerceIn(180f, 500f)
            }
            else -> 550f // Nominal soil baseline
        }
    }

    /**
     * Synthesizes and streams a pure sine waveform with smooth fade-in/fade-out envelope
     * to eliminate speaker pops and crackles.
     */
    private fun playTone(frequency: Float, durationMs: Int, volume: Float) {
        val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(numSamples)
        val attackSamples = (numSamples * 0.12).toInt().coerceAtLeast(1)
        val decaySamples = (numSamples * 0.20).toInt().coerceAtLeast(1)

        val angularFrequency = 2.0 * PI * frequency / sampleRate

        for (i in 0 until numSamples) {
            val sine = sin(angularFrequency * i)

            // Smooth envelope window (attack / sustain / decay)
            val envelope = when {
                i < attackSamples -> i.toDouble() / attackSamples
                i > numSamples - decaySamples -> (numSamples - i).toDouble() / decaySamples
                else -> 1.0
            }

            buffer[i] = (sine * envelope * Short.MAX_VALUE * volume).toInt().toShort()
        }

        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val trackBufferSize = maxOf(buffer.size * 2, minBufferSize)

        var audioTrack: AudioTrack? = null
        try {
            audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
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
                    .setBufferSizeInBytes(trackBufferSize)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                AudioTrack(
                    android.media.AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    trackBufferSize,
                    AudioTrack.MODE_STATIC
                )
            }

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Small sleep while audio buffer plays
            Thread.sleep(durationMs.toLong() + 10)
        } finally {
            try {
                audioTrack?.stop()
                audioTrack?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Plays a pleasant 3-tone ascending arpeggio (C5-E5-G5) to confirm hardware connection.
     */
    fun playConnectionChime() {
        audioExecutor.execute {
            try {
                playTone(frequency = 523.25f, durationMs = 60, volume = 0.5f) // C5
                playTone(frequency = 659.25f, durationMs = 60, volume = 0.55f) // E5
                playTone(frequency = 783.99f, durationMs = 110, volume = 0.6f) // G5
            } catch (_: Exception) {}
        }
    }

    /**
     * Announces hardware connection in the active language:
     * - Persian: "اتصال با دنیک برقرار شد"
     * - English: "Connected to Dnic System"
     *
     * In addition to voice TTS, also plays an acoustic chime.
     */
    fun announceConnection(isPersian: Boolean) {
        // Acoustic chime first
        playConnectionChime()

        // Voice announcement via TTS
        if (!isTtsInitialized || tts == null) return

        try {
            val textToSpeak: String
            val targetLocale: Locale

            if (isPersian) {
                textToSpeak = "اتصال با دنیک برقرار شد"
                targetLocale = Locale("fa")
            } else {
                textToSpeak = "Connected to Dnic System"
                targetLocale = Locale.US
            }

            val langResult = tts?.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_AVAILABLE ||
                langResult == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                langResult == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null, "connection_announcement")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak(textToSpeak, TextToSpeech.QUEUE_ADD, null)
                }
            } else if (isPersian) {
                // If Persian TTS voice pack is not installed on the system, announce in English as fallback
                tts?.setLanguage(Locale.US)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts?.speak("Connected to Dnic", TextToSpeech.QUEUE_ADD, null, "connection_fallback")
                } else {
                    @Suppress("DEPRECATION")
                    tts?.speak("Connected to Dnic", TextToSpeech.QUEUE_ADD, null)
                }
            }
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        audioExecutor.shutdown()
    }
}
