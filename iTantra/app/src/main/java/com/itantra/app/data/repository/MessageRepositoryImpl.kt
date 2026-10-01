package com.itantra.app.data.repository

import com.itantra.app.data.database.MessageDao
import com.itantra.app.data.database.MessageEntity
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.PlaybackStatus
import com.itantra.app.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepositoryImpl(
    private val messageDao: MessageDao
) : MessageRepository {

    override fun getAllMessages(): Flow<List<Message>> {
        return messageDao.getAllMessages().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMessagesByConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesByConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getPendingQueue(): Flow<List<Message>> {
        return messageDao.getPendingQueue().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getMessageById(messageId: String): Message? {
        return messageDao.getMessageById(messageId)?.toDomain()
    }

    override suspend fun insertMessage(message: Message) {
        messageDao.insertMessage(MessageEntity.fromDomain(message))
    }

    override suspend fun updateDeliveryStatus(messageId: String, status: DeliveryStatus, errorMsg: String?) {
        messageDao.updateDeliveryStatus(messageId, status.name, errorMsg)
    }

    override suspend fun updatePlaybackStatus(messageId: String, status: PlaybackStatus) {
        messageDao.updatePlaybackStatus(messageId, status.name)
    }

    override suspend fun incrementRetryCount(messageId: String) {
        messageDao.incrementRetryCount(messageId)
    }

    override suspend fun deleteMessage(messageId: String) {
        messageDao.deleteMessage(messageId)
    }

    override suspend fun clearAllMessages() {
        messageDao.clearAllMessages()
    }

    override suspend fun getPendingCount(): Int {
        return messageDao.getPendingCount()
    }
}
