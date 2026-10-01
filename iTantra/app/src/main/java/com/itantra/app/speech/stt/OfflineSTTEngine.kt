package com.itantra.app.speech.stt

import com.itantra.app.domain.model.AppLanguage

data class STTResult(
    val text: String,
    val confidence: Float,
    val language: AppLanguage,
    val processingTimeMs: Long,
    val isLowConfidence: Boolean = confidence < 0.75f,
    val wordErrorRateEstimated: Float? = null,
    val engineName: String = "Offline Indic ASR"
)

interface OfflineSTTEngine {
    suspend fun initialize(language: AppLanguage): Result<Unit>
    suspend fun transcribe(pcmAudio: ShortArray, language: AppLanguage): STTResult
    fun isReady(): Boolean
    fun getSupportedLanguages(): List<AppLanguage>
    fun release()
}
