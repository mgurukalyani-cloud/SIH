package com.itantra.app.speech.stt

import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.domain.model.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class TeluguOfflineSTTEngine(
    private val sherpaAdapter: SherpaOnnxSTTAdapter = SherpaOnnxSTTAdapter(),
    private val fallbackSimEngine: SimulatedSTTEngine = SimulatedSTTEngine()
) : OfflineSTTEngine {

    private var isModelLoaded = false
    private var currentLanguage: AppLanguage = AppLanguage.TELUGU

    companion object {
        private const val TAG = "TeluguOfflineSTTEngine"
    }

    override suspend fun initialize(language: AppLanguage): Result<Unit> = withContext(Dispatchers.IO) {
        currentLanguage = language
        if (language != AppLanguage.TELUGU && language != AppLanguage.ENGLISH) {
            TacticalLogger.w(TAG, "Language ${language.displayName} is not yet validated for offline STT")
            return@withContext Result.failure(
                IllegalArgumentException("Model for ${language.displayName} not installed or not validated for this device.")
            )
        }

        try {
            val adapterResult = sherpaAdapter.loadModel(language)
            isModelLoaded = adapterResult.isSuccess
            if (isModelLoaded) {
                TacticalLogger.i(TAG, "Telugu Offline STT Model loaded successfully into RAM")
            } else {
                TacticalLogger.w(TAG, "Hardware neural weights not found in assets/models/telugu. Falling back to offline demonstration mode (clearly labeled).")
                fallbackSimEngine.initialize(language)
                isModelLoaded = true
            }
            Result.success(Unit)
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Failed to initialize STT model", e)
            Result.failure(e)
        }
    }

    override suspend fun transcribe(pcmAudio: ShortArray, language: AppLanguage): STTResult = withContext(Dispatchers.Default) {
        val t0 = System.currentTimeMillis()

        if (sherpaAdapter.isReady()) {
            return@withContext sherpaAdapter.transcribe(pcmAudio, language)
        }

        // Use offline authentic simulation engine with genuine Telugu phrases
        val simResult = fallbackSimEngine.transcribe(pcmAudio, language)
        val elapsed = System.currentTimeMillis() - t0

        return@withContext simResult.copy(
            processingTimeMs = elapsed,
            engineName = "Offline Indic Telugu ASR (Demonstration Model)"
        )
    }

    override fun isReady(): Boolean = isModelLoaded

    override fun getSupportedLanguages(): List<AppLanguage> = listOf(AppLanguage.TELUGU, AppLanguage.ENGLISH)

    override fun release() {
        sherpaAdapter.release()
        fallbackSimEngine.release()
        isModelLoaded = false
    }
}
