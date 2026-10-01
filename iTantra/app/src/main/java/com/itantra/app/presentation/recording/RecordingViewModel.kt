package com.itantra.app.presentation.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.core.common.AppConstants
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.core.result.AppResult
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.usecase.TranscribeAudioUseCase
import com.itantra.app.domain.usecase.TransmitMessageUseCase
import com.itantra.app.speech.audio.AudioRecorder
import com.itantra.app.speech.vad.VadState
import com.itantra.app.speech.vad.VoiceActivityDetector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RecordingUiState(
    val isRecording: Boolean = false,
    val isProcessingStt: Boolean = false,
    val vadState: VadState = VadState.SILENCE,
    val currentRms: Float = 0f,
    val recognizedText: String? = null,
    val confidenceScore: Float = 0f,
    val language: AppLanguage = AppLanguage.TELUGU,
    val showLowConfidenceDialog: Boolean = false,
    val showConfirmationCard: Boolean = false,
    val payloadSizeBytes: Int = 0,
    val estimatedTransmissionTimeMs: Long = 0,
    val errorMessage: String? = null
)

class RecordingViewModel(
    private val prefs: AppPreferences,
    private val audioRecorder: AudioRecorder = AudioRecorder(),
    private val vad: VoiceActivityDetector = VoiceActivityDetector(),
    private val transcribeAudioUseCase: TranscribeAudioUseCase,
    private val transmitMessageUseCase: TransmitMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordingUiState(language = prefs.selectedLanguage))
    val uiState: StateFlow<RecordingUiState> = _uiState.asStateFlow()

    private val capturedPcmBuffer = ArrayList<Short>()
    private var recordingStartTimeMs: Long = 0L

    // Concurrency & Deduplication Safeguards
    private val isRecordingActive = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isSttActive = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isTransmittingActive = java.util.concurrent.atomic.AtomicBoolean(false)
    private var sessionCounter = 0L
    private var currentSessionId = 0L
    private var sttJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "RecordingViewModel"
    }

    fun startPtt() {
        // Safeguard 1: Prevent duplicate recording start if already recording or in STT
        if (!isRecordingActive.compareAndSet(false, true)) {
            TacticalLogger.w(TAG, "Duplicate startPtt ignored: Recording is already in progress.")
            return
        }

        // Safeguard 2: Cancel any lingering STT job from a prior session
        sttJob?.cancel()
        isSttActive.set(false)
        isTransmittingActive.set(false)

        currentSessionId = ++sessionCounter
        recordingStartTimeMs = System.currentTimeMillis()

        synchronized(capturedPcmBuffer) {
            capturedPcmBuffer.clear()
        }
        vad.reset()

        TacticalLogger.i(TAG, "[iTantra-Trace] RECORDING_STARTED (sessionId=$currentSessionId, lang=${prefs.selectedLanguage.displayName})")

        _uiState.value = _uiState.value.copy(
            isRecording = true,
            isProcessingStt = false,
            recognizedText = null,
            showConfirmationCard = false,
            showLowConfidenceDialog = false,
            errorMessage = null,
            vadState = VadState.SILENCE
        )

        audioRecorder.startRecording(
            onFrameCaptured = { frame, rms ->
                synchronized(capturedPcmBuffer) {
                    for (sample in frame) capturedPcmBuffer.add(sample)
                }
                val vadResult = vad.processFrame(frame)
                _uiState.value = _uiState.value.copy(
                    currentRms = rms,
                    vadState = vadResult.state
                )
            },
            onError = { error ->
                TacticalLogger.e(TAG, "AudioRecorder error on session $currentSessionId: $error")
                isRecordingActive.set(false)
                _uiState.value = _uiState.value.copy(
                    isRecording = false,
                    errorMessage = error
                )
            }
        )
    }

    fun stopPtt() {
        // Safeguard 3: Only process stop if recording was actively running
        if (!isRecordingActive.compareAndSet(true, false)) {
            TacticalLogger.w(TAG, "Duplicate stopPtt ignored: Recorder is not currently active.")
            return
        }

        val sessionId = currentSessionId
        audioRecorder.stopRecording()
        _uiState.value = _uiState.value.copy(isRecording = false)

        val durationMs = System.currentTimeMillis() - recordingStartTimeMs
        val pcmArray = synchronized(capturedPcmBuffer) { capturedPcmBuffer.toShortArray() }

        TacticalLogger.i(TAG, "[iTantra-Trace] RECORDING_STOPPED (sessionId=$sessionId, durationMs=$durationMs, samples=${pcmArray.size})")

        // Safeguard 4: VAD Sanity Check - If silence, short noise, or empty
        if (!vad.hasMetMinimumSpeech() || pcmArray.isEmpty() || durationMs < 300) {
            TacticalLogger.w(TAG, "VAD: Silence or noise transient detected. Dropping audio (no false emergency triggered).")
            _uiState.value = _uiState.value.copy(
                errorMessage = "No voice detected. Please hold the button and speak clearly."
            )
            return
        }

        // Run Single Offline STT Conversion
        processSpeechToText(pcmArray, sessionId)
    }

    private fun processSpeechToText(pcmArray: ShortArray, sessionId: Long) {
        // Safeguard 5: Ensure strictly one STT job runs per session
        if (!isSttActive.compareAndSet(false, true)) {
            TacticalLogger.w(TAG, "Duplicate STT request ignored for session $sessionId.")
            return
        }

        _uiState.value = _uiState.value.copy(isProcessingStt = true)
        TacticalLogger.i(TAG, "[iTantra-Trace] STT_STARTED (sessionId=$sessionId, lang=${prefs.selectedLanguage.displayName}, pcmSamples=${pcmArray.size})")

        sttJob?.cancel()
        sttJob = viewModelScope.launch {
            try {
                val result = transcribeAudioUseCase(pcmArray, prefs.selectedLanguage)
                _uiState.value = _uiState.value.copy(isProcessingStt = false)

                when (result) {
                    is AppResult.Success -> {
                        val stt = result.data

                        // Safeguard 6: Check for empty or blank speech result
                        if (stt.text.isBlank()) {
                            TacticalLogger.w(TAG, "STT returned blank transcript for session $sessionId.")
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Could not recognize speech in ${prefs.selectedLanguage.displayName}. Please verify language or speak clearly."
                            )
                            return@launch
                        }

                        TacticalLogger.i(TAG, "[iTantra-Trace] STT_COMPLETED (sessionId=$sessionId, text='${stt.text}', conf=${stt.confidence})")

                        val payloadBytes = stt.text.toByteArray(Charsets.UTF_8).size
                        val estTimeMs = (payloadBytes * 8L * 1000L) / 1200L // 1.2 kbps transmission time

                        _uiState.value = _uiState.value.copy(
                            recognizedText = stt.text,
                            confidenceScore = stt.confidence,
                            payloadSizeBytes = payloadBytes,
                            estimatedTransmissionTimeMs = estTimeMs,
                            showConfirmationCard = true,
                            showLowConfidenceDialog = stt.isLowConfidence
                        )
                    }
                    is AppResult.Error -> {
                        TacticalLogger.e(TAG, "STT conversion error on session $sessionId: ${result.message}")
                        _uiState.value = _uiState.value.copy(
                            errorMessage = result.message
                        )
                    }
                    is AppResult.Loading -> {}
                }
            } finally {
                isSttActive.set(false)
            }
        }
    }

    fun confirmAndSend(editedText: String? = null, isSos: Boolean = false) {
        val textToSend = editedText ?: _uiState.value.recognizedText ?: return

        // Safeguard 7: Atomic guard against rapid double-clicks on Transmit button
        if (!isTransmittingActive.compareAndSet(false, true)) {
            TacticalLogger.w(TAG, "Duplicate transmit ignored: Message is already being dispatched.")
            return
        }

        // Immediately clear confirmation state to prevent re-triggering from UI
        val currentConfidence = _uiState.value.confidenceScore
        _uiState.value = _uiState.value.copy(
            showConfirmationCard = false,
            showLowConfidenceDialog = false,
            recognizedText = null
        )

        viewModelScope.launch {
            try {
                val msg = Message(
                    senderId = prefs.nodeCallsign,
                    text = textToSend,
                    language = prefs.selectedLanguage,
                    priority = if (isSos) MessagePriority.EMERGENCY else MessagePriority.NORMAL,
                    confidenceScore = currentConfidence,
                    deliveryStatus = DeliveryStatus.QUEUED
                )

                TacticalLogger.i(TAG, "[iTantra-Trace] MESSAGE_TRANSMITTED (msgId=${msg.messageId}, lang=${msg.language.displayName}, prio=${msg.priority})")

                transmitMessageUseCase(
                    message = msg,
                    sourceNodeId = 0xA89F.toShort(),
                    enableCompression = prefs.isCompressionEnabled
                )
            } finally {
                isTransmittingActive.set(false)
            }
        }
    }

    fun dismissConfirmation() {
        sttJob?.cancel()
        isSttActive.set(false)
        isTransmittingActive.set(false)
        _uiState.value = _uiState.value.copy(
            showConfirmationCard = false,
            showLowConfidenceDialog = false,
            recognizedText = null
        )
    }

    override fun onCleared() {
        super.onCleared()
        sttJob?.cancel()
        isRecordingActive.set(false)
        isSttActive.set(false)
        isTransmittingActive.set(false)
        audioRecorder.stopRecording()
    }
}
