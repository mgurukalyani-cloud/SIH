package com.itantra.app.domain.repository

import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.PlaybackStatus
import kotlinx.coroutines.flow.Flow

interface MessageRepository {
    fun getAllMessages(): Flow<List<Message>>
    fun getMessagesByConversation(conversationId: String): Flow<List<Message>>
    fun getPendingQueue(): Flow<List<Message>>
    suspend fun getMessageById(messageId: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun updateDeliveryStatus(messageId: String, status: DeliveryStatus, errorMsg: String? = null)
    suspend fun updatePlaybackStatus(messageId: String, status: PlaybackStatus)
    suspend fun incrementRetryCount(messageId: String)
    suspend fun deleteMessage(messageId: String)
    suspend fun clearAllMessages()
    suspend fun getPendingCount(): Int
}
