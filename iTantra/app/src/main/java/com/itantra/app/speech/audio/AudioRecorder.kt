package com.itantra.app.speech.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.itantra.app.core.common.AppConstants
import com.itantra.app.core.logging.TacticalLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AudioRecorder(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) {
    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _currentRms = MutableStateFlow(0f)
    val currentRms: StateFlow<Float> = _currentRms.asStateFlow()

    companion object {
        private const val TAG = "AudioRecorder"
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    }

    @SuppressLint("MissingPermission")
    fun startRecording(
        onFrameCaptured: (ShortArray, Float) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isRecording.value) return

        recordJob?.cancel()
        recordJob = scope.launch {
            val minBufSize = AudioRecord.getMinBufferSize(
                AppConstants.AUDIO_SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            )
            val bufferSize = maxOf(minBufSize, AppConstants.SAMPLES_PER_FRAME * 4)

            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    AppConstants.AUDIO_SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    bufferSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    TacticalLogger.e(TAG, "AudioRecord initialization failed")
                    onError("Microphone hardware failed to initialize")
                    return@launch
                }

                audioRecord?.startRecording()
                _isRecording.value = true
                TacticalLogger.i(TAG, "PCM recording started at ${AppConstants.AUDIO_SAMPLE_RATE} Hz")

                val frameBuffer = ShortArray(AppConstants.SAMPLES_PER_FRAME)

                while (isActive && _isRecording.value) {
                    val read = audioRecord?.read(frameBuffer, 0, frameBuffer.size) ?: 0
                    if (read > 0) {
                        // Calculate RMS Energy
                        var sum = 0.0
                        for (i in 0 until read) {
                            sum += frameBuffer[i] * frameBuffer[i]
                        }
                        val rms = sqrt(sum / read).toFloat()
                        _currentRms.value = rms

                        val copy = frameBuffer.copyOf(read)
                        onFrameCaptured(copy, rms)
                    }
                }
            } catch (e: Exception) {
                TacticalLogger.e(TAG, "Exception during audio capture", e)
                onError("Audio recording error: ${e.localizedMessage}")
            } finally {
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        _isRecording.value = false
        recordJob?.cancel()
        recordJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (ignored: Exception) {}
        audioRecord = null
        _currentRms.value = 0f
        TacticalLogger.i(TAG, "Audio recording released")
    }
}
