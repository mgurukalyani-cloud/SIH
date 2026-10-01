package com.itantra.app.communication.queue

import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.domain.usecase.TransmitMessageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class StoreAndForwardQueue(
    private val messageRepository: MessageRepository,
    private val transport: CommunicationTransport,
    private val transmitMessageUseCase: TransmitMessageUseCase,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) {
    private var workerJob: Job? = null

    companion object {
        private const val TAG = "StoreAndForwardQueue"
        private const val RETRY_INTERVAL_MS = 5000L
    }

    private val isDraining = java.util.concurrent.atomic.AtomicBoolean(false)

    fun start() {
        workerJob?.cancel()
        workerJob = scope.launch {
            TacticalLogger.i(TAG, "Store-and-Forward background daemon started")
            while (isActive) {
                try {
                    val connState = transport.connectionState().first()
                    if (connState is ConnectionState.Connected) {
                        drainQueue()
                    }
                } catch (e: Exception) {
                    TacticalLogger.w(TAG, "Queue monitor exception: ${e.localizedMessage}")
                }
                delay(RETRY_INTERVAL_MS)
            }
        }
    }

    private suspend fun drainQueue() {
        // Safeguard 8: Prevent multiple queue consumers from running concurrently
        if (!isDraining.compareAndSet(false, true)) {
            return
        }

        try {
            val pendingMessages = messageRepository.getPendingQueue().first()
            val eligible = pendingMessages.filter { msg ->
                // Do not re-transmit messages that are actively being sent or too fresh (< 2000ms old)
                msg.deliveryStatus != DeliveryStatus.SENDING &&
                (System.currentTimeMillis() - msg.timestamp > 2000L)
            }

            if (eligible.isNotEmpty()) {
                TacticalLogger.i(TAG, "Carrier link online. Draining ${eligible.size} deferred message(s)...")
                for (msg in eligible) {
                    if (!transport.isConnected()) break
                    messageRepository.incrementRetryCount(msg.messageId)
                    val result = transmitMessageUseCase(
                        message = msg,
                        sourceNodeId = 0xA89F.toShort()
                    )
                    if (result.isSuccess) {
                        TacticalLogger.i(TAG, "Drained deferred message ${msg.messageId} successfully")
                    } else {
                        delay(1000)
                    }
                }
            }
        } finally {
            isDraining.set(false)
        }
    }

    fun stop() {
        workerJob?.cancel()
    }
}
