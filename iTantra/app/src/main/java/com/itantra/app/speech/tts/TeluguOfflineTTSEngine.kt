package com.itantra.app.speech.tts

import android.content.Context
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.domain.model.AppLanguage

class TeluguOfflineTTSEngine(
    private val context: Context,
    private val systemTts: AndroidSystemTTSEngine = AndroidSystemTTSEngine(context),
    private val simTts: SimulatedTTSEngine = SimulatedTTSEngine()
) : OfflineTTSEngine {

    private var activeEngine: OfflineTTSEngine = simTts
    private var isInitialized = false

    companion object {
        private const val TAG = "TeluguOfflineTTSEngine"
    }

    override suspend fun initialize(language: AppLanguage): Result<Unit> {
        if (language != AppLanguage.TELUGU && language != AppLanguage.ENGLISH) {
            return Result.failure(IllegalArgumentException("Model for ${language.displayName} not installed or not validated for this device."))
        }

        val sysResult = systemTts.initialize(language)
        activeEngine = if (sysResult.isSuccess) {
            TacticalLogger.i(TAG, "Using Android System offline TTS for ${language.displayName}")
            systemTts
        } else {
            TacticalLogger.w(TAG, "System Telugu voice data missing. Using Offline Indic TTS (Simulated Demonstration)")
            simTts.initialize(language)
            simTts
        }
        isInitialized = true
        return Result.success(Unit)
    }

    override suspend fun synthesize(text: String, language: AppLanguage): TTSResult {
        return activeEngine.synthesize(text, language)
    }

    override suspend fun play(text: String, language: AppLanguage, onComplete: () -> Unit) {
        activeEngine.play(text, language, onComplete)
    }

    override fun stop() {
        activeEngine.stop()
    }

    override fun isReady(): Boolean = isInitialized

    override fun getSupportedLanguages(): List<AppLanguage> = listOf(AppLanguage.TELUGU, AppLanguage.ENGLISH)

    override fun release() {
        systemTts.release()
        simTts.release()
        isInitialized = false
    }
}
