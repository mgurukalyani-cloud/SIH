package com.itantra.app.domain.usecase

import com.itantra.app.core.result.AppResult
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.speech.tts.OfflineTTSEngine
import com.itantra.app.speech.tts.TTSResult

class SynthesizeSpeechUseCase(
    private val ttsEngine: OfflineTTSEngine
) {
    suspend operator fun invoke(
        text: String,
        language: AppLanguage,
        autoPlay: Boolean = true,
        onComplete: () -> Unit = {}
    ): AppResult<TTSResult> {
        return try {
            val result = ttsEngine.synthesize(text, language)
            if (autoPlay) {
                ttsEngine.play(text, language, onComplete)
            }
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(e, "TTS Synthesis failed: ${e.localizedMessage}")
        }
    }

    fun stop() {
        ttsEngine.stop()
    }
}
