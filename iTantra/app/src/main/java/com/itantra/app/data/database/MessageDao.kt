package com.itantra.app.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesByConversation(conversationId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE deliveryStatusName IN ('QUEUED', 'PENDING', 'FAILED') ORDER BY timestamp ASC")
    fun getPendingQueue(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET deliveryStatusName = :status, errorMessage = :errorMsg WHERE messageId = :messageId")
    suspend fun updateDeliveryStatus(messageId: String, status: String, errorMsg: String?)

    @Query("UPDATE messages SET playbackStatusName = :status WHERE messageId = :messageId")
    suspend fun updatePlaybackStatus(messageId: String, status: String)

    @Query("UPDATE messages SET retryCount = retryCount + 1 WHERE messageId = :messageId")
    suspend fun incrementRetryCount(messageId: String)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessage(messageId: String)

    @Query("DELETE FROM messages")
    suspend fun clearAllMessages()

    @Query("SELECT COUNT(*) FROM messages WHERE deliveryStatusName IN ('QUEUED', 'PENDING', 'FAILED')")
    suspend fun getPendingCount(): Int
}
