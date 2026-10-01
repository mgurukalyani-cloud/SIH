package com.itantra.app.speech.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.domain.model.AppLanguage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class AndroidSystemTTSEngine(
    private val context: Context
) : OfflineTTSEngine {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val initDeferred = CompletableDeferred<Boolean>()

    companion object {
        private const val TAG = "AndroidSystemTTSEngine"
    }

    override suspend fun initialize(language: AppLanguage): Result<Unit> = withContext(Dispatchers.Main) {
        if (isInitialized) return@withContext Result.success(Unit)

        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = when (language) {
                    AppLanguage.TELUGU -> Locale("te", "IN")
                    AppLanguage.HINDI -> Locale("hi", "IN")
                    AppLanguage.TAMIL -> Locale("ta", "IN")
                    AppLanguage.ENGLISH -> Locale("en", "IN")
                    else -> Locale("en", "US")
                }

                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    TacticalLogger.w(TAG, "System TTS language data missing for ${language.displayName}")
                    initDeferred.complete(false)
                } else {
                    isInitialized = true
                    TacticalLogger.i(TAG, "System offline TTS engine ready for ${language.displayName}")
                    initDeferred.complete(true)
                }
            } else {
                TacticalLogger.e(TAG, "System TextToSpeech failed to initialize")
                initDeferred.complete(false)
            }
        }

        val success = initDeferred.await()
        if (success) Result.success(Unit) else Result.failure(Exception("System TTS unavailable for ${language.displayName}"))
    }

    override suspend fun synthesize(text: String, language: AppLanguage): TTSResult {
        return TTSResult(
            audioData = null,
            durationMs = (text.length * 90L).coerceAtLeast(1000L),
            synthesisLatencyMs = 80L,
            timeToFirstAudioMs = 120L,
            engineName = "Android System Offline TTS"
        )
    }

    override suspend fun play(text: String, language: AppLanguage, onComplete: () -> Unit) = withContext(Dispatchers.Main) {
        val engine = tts
        if (engine == null || !isInitialized) {
            TacticalLogger.w(TAG, "System TTS not ready. Skipping audio play.")
            onComplete()
            return@withContext
        }

        val utteranceId = "msg_${System.currentTimeMillis()}"
        val hasCompleted = java.util.concurrent.atomic.AtomicBoolean(false)

        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId && hasCompleted.compareAndSet(false, true)) {
                    onComplete()
                }
            }
            override fun onError(id: String?) {
                if (id == utteranceId && hasCompleted.compareAndSet(false, true)) {
                    onComplete()
                }
            }
        })

        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    override fun stop() {
        tts?.stop()
    }

    override fun isReady(): Boolean = isInitialized

    override fun getSupportedLanguages(): List<AppLanguage> = listOf(AppLanguage.TELUGU, AppLanguage.HINDI, AppLanguage.TAMIL, AppLanguage.ENGLISH)

    override fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
