package com.itantra.app.domain.model

import java.util.UUID

data class Message(
    val messageId: String = UUID.randomUUID().toString(),
    val conversationId: String = "default_channel",
    val senderId: String,
    val receiverId: String = "broadcast",
    val text: String,
    val language: AppLanguage,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: MessagePriority = MessagePriority.NORMAL,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.DRAFT,
    val playbackStatus: PlaybackStatus = PlaybackStatus.UNPLAYED,
    val isIncoming: Boolean = false,
    val retryCount: Int = 0,
    val checksum: Int = 0,
    val originalSizeBytes: Int = text.toByteArray(Charsets.UTF_8).size,
    val compressedSizeBytes: Int = originalSizeBytes,
    val audioDurationMs: Long = 0L,
    val confidenceScore: Float = 1.0f,
    val errorMessage: String? = null
) {
    val isEmergency: Boolean get() = priority == MessagePriority.EMERGENCY
}
