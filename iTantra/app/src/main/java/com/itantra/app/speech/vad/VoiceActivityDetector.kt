package com.itantra.app.speech.vad

import com.itantra.app.core.common.AppConstants
import kotlin.math.sqrt

enum class VadState {
    SILENCE,
    SPEECH_STARTED,
    IN_SPEECH,
    SPEECH_ENDED
}

data class VadResult(
    val state: VadState,
    val rmsEnergy: Float,
    val zeroCrossingRate: Float,
    val isVoiced: Boolean,
    val voicedFramesCount: Int,
    val totalFramesCount: Int
)

class VoiceActivityDetector(
    var energyThreshold: Double = AppConstants.DEFAULT_VAD_ENERGY_THRESHOLD,
    var silenceHangoverMs: Long = AppConstants.DEFAULT_SILENCE_HANGOVER_MS,
    var minVoicedFramesRequired: Int = AppConstants.MIN_VOICED_FRAMES_REQUIRED,
    var maxUtteranceDurationMs: Long = 15000L // 15 seconds max
) {
    private var isSpeechActive = false
    private var lastVoicedTimestamp: Long = 0L
    private var utteranceStartTime: Long = 0L
    private var voicedFramesCount = 0
    private var totalFramesCount = 0

    fun reset() {
        isSpeechActive = false
        lastVoicedTimestamp = 0L
        utteranceStartTime = 0L
        voicedFramesCount = 0
        totalFramesCount = 0
    }

    fun processFrame(pcmFrame: ShortArray): VadResult {
        totalFramesCount++
        val now = System.currentTimeMillis()

        // 1. Calculate RMS Energy
        var sumSquares = 0.0
        var zeroCrossings = 0
        for (i in pcmFrame.indices) {
            val sample = pcmFrame[i].toInt()
            sumSquares += sample * sample
            if (i > 0 && ((sample >= 0 && pcmFrame[i - 1] < 0) || (sample < 0 && pcmFrame[i - 1] >= 0))) {
                zeroCrossings++
            }
        }
        val rms = sqrt(sumSquares / pcmFrame.size).toFloat()
        val zcr = zeroCrossings.toFloat() / pcmFrame.size

        val isVoiced = rms > energyThreshold

        if (isVoiced) {
            voicedFramesCount++
            lastVoicedTimestamp = now
        }

        val state: VadState
        if (!isSpeechActive) {
            if (isVoiced && voicedFramesCount >= 2) {
                isSpeechActive = true
                utteranceStartTime = now
                state = VadState.SPEECH_STARTED
            } else {
                state = VadState.SILENCE
            }
        } else {
            val silenceDuration = now - lastVoicedTimestamp
            val utteranceDuration = now - utteranceStartTime

            if (utteranceDuration >= maxUtteranceDurationMs) {
                isSpeechActive = false
                state = VadState.SPEECH_ENDED
            } else if (silenceDuration > silenceHangoverMs) {
                isSpeechActive = false
                state = VadState.SPEECH_ENDED
            } else {
                state = VadState.IN_SPEECH
            }
        }

        return VadResult(
            state = state,
            rmsEnergy = rms,
            zeroCrossingRate = zcr,
            isVoiced = isVoiced,
            voicedFramesCount = voicedFramesCount,
            totalFramesCount = totalFramesCount
        )
    }

    fun hasMetMinimumSpeech(): Boolean {
        return voicedFramesCount >= minVoicedFramesRequired
    }
}
