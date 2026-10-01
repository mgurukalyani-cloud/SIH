package com.itantra.app.domain.usecase

import com.itantra.app.core.result.AppResult
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.speech.stt.OfflineSTTEngine
import com.itantra.app.speech.stt.STTResult

class TranscribeAudioUseCase(
    private val sttEngine: OfflineSTTEngine
) {
    suspend operator fun invoke(pcmAudio: ShortArray, language: AppLanguage): AppResult<STTResult> {
        return try {
            val result = sttEngine.transcribe(pcmAudio, language)
            AppResult.Success(result)
        } catch (e: Exception) {
            AppResult.Error(e, "STT Transcription failed: ${e.localizedMessage}")
        }
    }
}
