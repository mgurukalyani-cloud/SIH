package com.itantra.app.speech.tts

import com.itantra.app.core.common.AppConstants
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.speech.audio.AudioPlayer
import kotlinx.coroutines.delay
import kotlin.math.sin

class SimulatedTTSEngine(
    private val audioPlayer: AudioPlayer = AudioPlayer()
) : OfflineTTSEngine {

    private var isReady = false

    override suspend fun initialize(language: AppLanguage): Result<Unit> {
        isReady = true
        return Result.success(Unit)
    }

    override suspend fun synthesize(text: String, language: AppLanguage): TTSResult {
        delay(120) // Realistic synthesis latency
        val sampleRate = AppConstants.AUDIO_SAMPLE_RATE
        val durationSeconds = (text.length * 0.08f).coerceIn(1.5f, 5.0f)
        val numSamples = (sampleRate * durationSeconds).toInt()

        return TTSResult(
            audioData = ByteArray(numSamples * 2),
            durationMs = (durationSeconds * 1000).toLong(),
            synthesisLatencyMs = 120L,
            timeToFirstAudioMs = 150L,
            sampleRate = sampleRate,
            engineName = "Offline Indic TTS (Simulated Engine)"
        )
    }

    override suspend fun play(text: String, language: AppLanguage, onComplete: () -> Unit) {
        val sampleRate = AppConstants.AUDIO_SAMPLE_RATE
        val durationSeconds = (text.length * 0.07f).coerceIn(1.2f, 4.0f)
        val numSamples = (sampleRate * durationSeconds).toInt()
        val pcm = ShortArray(numSamples)

        // Generate pleasant harmonic multi-tone acoustic notification
        val f1 = 440.0 // A4
        val f2 = 880.0 // A5
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val envelope = (1.0 - (i.toDouble() / numSamples)) // smooth decay
            val wave = 0.5 * sin(2 * Math.PI * f1 * t) + 0.5 * sin(2 * Math.PI * f2 * t)
            pcm[i] = (wave * envelope * Short.MAX_VALUE * 0.4).toInt().toShort()
        }

        audioPlayer.playPcm(pcm, sampleRate, onComplete)
    }

    override fun stop() {
        audioPlayer.stop()
    }

    override fun isReady(): Boolean = isReady

    override fun getSupportedLanguages(): List<AppLanguage> = listOf(AppLanguage.TELUGU, AppLanguage.ENGLISH)

    override fun release() {
        audioPlayer.stop()
        isReady = false
    }
}
