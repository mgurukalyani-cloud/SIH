package com.itantra.app.speech.tts

import com.itantra.app.domain.model.AppLanguage

data class TTSResult(
    val audioData: ByteArray?,
    val durationMs: Long,
    val synthesisLatencyMs: Long,
    val timeToFirstAudioMs: Long,
    val sampleRate: Int = 16000,
    val engineName: String = "Offline Indic TTS"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TTSResult
        return audioData.contentEquals(other.audioData)
    }

    override fun hashCode(): Int {
        return audioData?.contentHashCode() ?: 0
    }
}

interface OfflineTTSEngine {
    suspend fun initialize(language: AppLanguage): Result<Unit>
    suspend fun synthesize(text: String, language: AppLanguage): TTSResult
    suspend fun play(text: String, language: AppLanguage, onComplete: () -> Unit = {})
    fun stop()
    fun isReady(): Boolean
    fun getSupportedLanguages(): List<AppLanguage>
    fun release()
}
