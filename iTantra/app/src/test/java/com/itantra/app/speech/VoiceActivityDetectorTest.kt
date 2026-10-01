package com.itantra.app.speech

import com.itantra.app.speech.vad.VadState
import com.itantra.app.speech.vad.VoiceActivityDetector
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.sin

class VoiceActivityDetectorTest {

    @Test
    fun testSilenceDetection() {
        val vad = VoiceActivityDetector(energyThreshold = 400.0)
        val silentFrame = ShortArray(480) { 0 } // Absolute zero silence

        val result = vad.processFrame(silentFrame)
        assertEquals(VadState.SILENCE, result.state)
        assertEquals(0f, result.rmsEnergy, 0.001f)
        assertFalse(vad.hasMetMinimumSpeech())
    }

    @Test
    fun testSpeechTriggersActiveState() {
        val vad = VoiceActivityDetector(energyThreshold = 400.0)
        val voiceFrame = ShortArray(480) { i ->
            (sin(i * 0.1) * 3000).toInt().toShort() // ~2121 RMS
        }

        vad.processFrame(voiceFrame)
        val result2 = vad.processFrame(voiceFrame)

        assertTrue(result2.rmsEnergy > 400.0)
        assertTrue(result2.isVoiced)
        assertTrue(result2.state == VadState.SPEECH_STARTED || result2.state == VadState.IN_SPEECH)
    }
}
