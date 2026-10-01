package com.itantra.app.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.MessagePriority
import com.itantra.app.domain.model.PlaybackStatus

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val text: String,
    val languageCode: String,
    val timestamp: Long,
    val priorityValue: Int,
    val deliveryStatusName: String,
    val playbackStatusName: String,
    val isIncoming: Boolean,
    val retryCount: Int,
    val checksum: Int,
    val originalSizeBytes: Int,
    val compressedSizeBytes: Int,
    val audioDurationMs: Long,
    val confidenceScore: Float,
    val errorMessage: String?
) {
    fun toDomain(): Message {
        return Message(
            messageId = messageId,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            text = text,
            language = AppLanguage.fromCode(languageCode),
            timestamp = timestamp,
            priority = MessagePriority.fromValue(priorityValue),
            deliveryStatus = DeliveryStatus.valueOf(deliveryStatusName),
            playbackStatus = PlaybackStatus.valueOf(playbackStatusName),
            isIncoming = isIncoming,
            retryCount = retryCount,
            checksum = checksum,
            originalSizeBytes = originalSizeBytes,
            compressedSizeBytes = compressedSizeBytes,
            audioDurationMs = audioDurationMs,
            confidenceScore = confidenceScore,
            errorMessage = errorMessage
        )
    }

    companion object {
        fun fromDomain(msg: Message): MessageEntity {
            return MessageEntity(
                messageId = msg.messageId,
                conversationId = msg.conversationId,
                senderId = msg.senderId,
                receiverId = msg.receiverId,
                text = msg.text,
                languageCode = msg.language.code,
                timestamp = msg.timestamp,
                priorityValue = msg.priority.value,
                deliveryStatusName = msg.deliveryStatus.name,
                playbackStatusName = msg.playbackStatus.name,
                isIncoming = msg.isIncoming,
                retryCount = msg.retryCount,
                checksum = msg.checksum,
                originalSizeBytes = msg.originalSizeBytes,
                compressedSizeBytes = msg.compressedSizeBytes,
                audioDurationMs = msg.audioDurationMs,
                confidenceScore = msg.confidenceScore,
                errorMessage = msg.errorMessage
            )
        }
    }
}
