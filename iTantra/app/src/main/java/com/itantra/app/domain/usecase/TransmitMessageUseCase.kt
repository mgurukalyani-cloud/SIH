package com.itantra.app.domain.usecase

import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.core.result.AppResult
import com.itantra.app.communication.compression.TextCompressor
import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.repository.MessageRepository

class TransmitMessageUseCase(
    private val messageRepository: MessageRepository,
    private val transport: CommunicationTransport
) {
    companion object {
        private const val TAG = "TransmitMessageUseCase"
    }

    suspend operator fun invoke(
        message: Message,
        sourceNodeId: Short,
        enableCompression: Boolean = true
    ): AppResult<Unit> {
        return try {
            // 1. Initial State: Directly set to SENDING so background queue does not pick it up concurrently
            messageRepository.insertMessage(message.copy(deliveryStatus = DeliveryStatus.SENDING))

            // 2. Compress payload if enabled
            val (payloadBytes, compType) = if (enableCompression) {
                TextCompressor.compress(message.text)
            } else {
                Pair(message.text.toByteArray(Charsets.UTF_8), iMFPPacket.CompressionType.RAW_UTF8)
            }

            val seqNum = (Math.abs(message.messageId.hashCode()) % 65535)

            // 3. Construct iMFP Frame
            val packet = iMFPPacket(
                version = 1,
                priority = message.priority,
                compressionType = compType,
                language = message.language,
                sequenceNumber = seqNum,
                sourceNodeId = sourceNodeId,
                payload = payloadBytes
            )

            // 4. Transmit across active physical/simulated link
            val sendResult = transport.send(packet)

            if (sendResult.isSuccess) {
                TacticalLogger.i(TAG, "[iTantra-Trace] MESSAGE_TRANSMITTED (msgId=${message.messageId}, seq=$seqNum, bytes=${packet.totalFrameSize}, transport=${transport.getTransportType()})")
                messageRepository.updateDeliveryStatus(message.messageId, DeliveryStatus.SENT)
                AppResult.Success(Unit)
            } else {
                val error = sendResult.exceptionOrNull()?.localizedMessage ?: "Link unavailable"
                TacticalLogger.w(TAG, "Link unavailable for ${message.messageId}. Saved to Store-and-Forward queue as PENDING.")
                messageRepository.updateDeliveryStatus(message.messageId, DeliveryStatus.PENDING, error)
                AppResult.Error(Exception(error), error)
            }
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Transmission pipeline error for ${message.messageId}", e)
            messageRepository.updateDeliveryStatus(message.messageId, DeliveryStatus.FAILED, e.localizedMessage)
            AppResult.Error(e, e.localizedMessage ?: "Failed to transmit message")
        }
    }
}
