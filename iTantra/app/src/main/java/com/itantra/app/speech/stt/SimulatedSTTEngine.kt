package com.itantra.app.speech.stt

import com.itantra.app.domain.model.AppLanguage
import kotlinx.coroutines.delay
import kotlin.math.sqrt

class SimulatedSTTEngine : OfflineSTTEngine {

    private var isReady = false
    private var simulatedSampleIndex = 0

    private val teluguEmergencyCorpus = listOf(
        "వరద నీరు పెరుగుతోంది, సహాయం కావాలి", // Flood water rising, need help
        "రక్షించండి, మేము 4వ అంతస్తులో చిక్కుకున్నాము", // Rescue us, trapped on 4th floor
        "వైద్య సహాయం తక్షణమే పంపండి", // Send medical aid immediately
        "వంతెన కూలిపోయింది, ప్రత్యామ్నాయ మార్గం చెప్పండి", // Bridge collapsed, provide alternate route
        "మంచినీరు మరియు ఆహార ప్యాకెట్లు అవసరం" // Drinking water and food packets needed
    )

    private val englishEmergencyCorpus = listOf(
        "Bridge collapsed at sector 4, need evacuation team immediately",
        "Medical aid required for three casualties",
        "Flash flood warning in low lying areas",
        "Communication repeater online, sending telemetry"
    )

    override suspend fun initialize(language: AppLanguage): Result<Unit> {
        isReady = true
        return Result.success(Unit)
    }

    override suspend fun transcribe(pcmAudio: ShortArray, language: AppLanguage): STTResult {
        // Realistic inference latency (RTF ~ 0.25)
        val audioDurationMs = (pcmAudio.size.toFloat() / 16000f * 1000f).toLong()
        val inferenceDelay = (audioDurationMs * 0.25f).toLong().coerceIn(150L, 800L)
        delay(inferenceDelay)

        // Calculate RMS of incoming audio to simulate confidence
        var sum = 0.0
        for (i in pcmAudio.indices) {
            sum += pcmAudio[i] * pcmAudio[i]
        }
        val rms = if (pcmAudio.isNotEmpty()) sqrt(sum / pcmAudio.size) else 0.0

        val (recognizedText, confidence) = if (language == AppLanguage.TELUGU) {
            val text = teluguEmergencyCorpus[simulatedSampleIndex % teluguEmergencyCorpus.size]
            simulatedSampleIndex++
            // Slightly lower confidence if audio was faint
            val conf = if (rms < 300) 0.68f else 0.94f
            Pair(text, conf)
        } else {
            val text = englishEmergencyCorpus[simulatedSampleIndex % englishEmergencyCorpus.size]
            simulatedSampleIndex++
            val conf = if (rms < 300) 0.70f else 0.96f
            Pair(text, conf)
        }

        return STTResult(
            text = recognizedText,
            confidence = confidence,
            language = language,
            processingTimeMs = inferenceDelay,
            isLowConfidence = confidence < 0.75f,
            engineName = "Offline Indic STT (Simulated Demonstration)"
        )
    }

    override fun isReady(): Boolean = isReady

    override fun getSupportedLanguages(): List<AppLanguage> = listOf(AppLanguage.TELUGU, AppLanguage.ENGLISH)

    override fun release() {
        isReady = false
    }
}
