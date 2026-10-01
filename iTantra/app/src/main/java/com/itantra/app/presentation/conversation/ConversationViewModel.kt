package com.itantra.app.presentation.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.PlaybackStatus
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.domain.usecase.SynthesizeSpeechUseCase
import com.itantra.app.domain.usecase.TransmitMessageUseCase
import com.itantra.app.data.local.AppPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationViewModel(
    private val prefs: AppPreferences,
    private val messageRepository: MessageRepository,
    private val synthesizeSpeechUseCase: SynthesizeSpeechUseCase,
    private val transmitMessageUseCase: TransmitMessageUseCase
) : ViewModel() {

    val messages: StateFlow<List<Message>> = messageRepository.getAllMessages().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    fun sendTextMessage(text: String, isSos: Boolean = false) {
        if (text.isBlank()) return

        viewModelScope.launch {
            val msg = Message(
                senderId = prefs.nodeCallsign,
                text = text.trim(),
                language = prefs.selectedLanguage,
                priority = if (isSos) com.itantra.app.domain.model.MessagePriority.EMERGENCY else com.itantra.app.domain.model.MessagePriority.NORMAL,
                deliveryStatus = DeliveryStatus.QUEUED
            )

            transmitMessageUseCase(
                message = msg,
                sourceNodeId = 0xA89F.toShort(),
                enableCompression = prefs.isCompressionEnabled
            )
        }
    }

    fun replayMessage(message: Message) {
        viewModelScope.launch {
            messageRepository.updatePlaybackStatus(message.messageId, PlaybackStatus.PLAYING)
            synthesizeSpeechUseCase(
                text = message.text,
                language = message.language,
                autoPlay = true,
                onComplete = {
                    viewModelScope.launch {
                        messageRepository.updatePlaybackStatus(message.messageId, PlaybackStatus.PLAYED)
                    }
                }
            )
        }
    }

    fun stopPlayback() {
        synthesizeSpeechUseCase.stop()
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            messageRepository.clearAllMessages()
        }
    }
}
