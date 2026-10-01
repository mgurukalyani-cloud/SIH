package com.itantra.app.speech.audio

import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.Message
import com.itantra.app.speech.tts.OfflineTTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class PlaybackQueue(
    private val ttsEngine: OfflineTTSEngine,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + Job())
) {
    private val queue = ConcurrentLinkedQueue<Message>()
    private var playbackJob: Job? = null

    private val _currentlyPlaying = MutableStateFlow<Message?>(null)
    val currentlyPlaying: StateFlow<Message?> = _currentlyPlaying.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    companion object {
        private const val TAG = "PlaybackQueue"
    }

    fun enqueue(message: Message, onMessagePlayed: (Message) -> Unit = {}) {
        // Safeguard 12: Ignore if this exact message is already queued or actively playing
        if (queue.any { it.messageId == message.messageId } || _currentlyPlaying.value?.messageId == message.messageId) {
            TacticalLogger.w(TAG, "Duplicate TTS playback suppressed: Message ${message.messageId} is already queued/playing.")
            return
        }

        if (message.isEmergency) {
            // High priority / Emergency: clear non-emergency and play immediately
            TacticalLogger.w(TAG, "EMERGENCY transmission received. Jumping to front of queue!")
            stop()
            queue.clear()
            queue.add(message)
        } else {
            queue.add(message)
        }
        processQueue(onMessagePlayed)
    }

    private fun processQueue(onMessagePlayed: (Message) -> Unit) {
        if (_isPlaying.value) return

        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive && queue.isNotEmpty()) {
                val nextMsg = queue.poll() ?: break
                _currentlyPlaying.value = nextMsg
                _isPlaying.value = true

                TacticalLogger.i(TAG, "Synthesizing and playing message ${nextMsg.messageId}: '${nextMsg.text}'")
                val completionDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()

                ttsEngine.play(nextMsg.text, nextMsg.language) {
                    onMessagePlayed(nextMsg)
                    completionDeferred.complete(Unit)
                }

                // Explicitly wait for utterance playback to finish before starting next message
                try {
                    completionDeferred.await()
                } catch (e: Exception) {
                    TacticalLogger.w(TAG, "Playback interrupted or canceled: ${e.localizedMessage}")
                } finally {
                    _currentlyPlaying.value = null
                    _isPlaying.value = false
                }
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        ttsEngine.stop()
        _currentlyPlaying.value = null
        _isPlaying.value = false
        TacticalLogger.i(TAG, "Playback stopped")
    }

    fun clear() {
        queue.clear()
        stop()
    }
}
