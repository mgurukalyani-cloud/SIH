package com.itantra.app

import android.app.Application
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.communication.compression.TextCompressor
import com.itantra.app.communication.packet.PacketFragmenter
import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.communication.queue.StoreAndForwardQueue
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.communication.transport.bluetooth.BluetoothTransport
import com.itantra.app.communication.transport.mock.MockLoopbackTransport
import com.itantra.app.communication.transport.wifi.LocalNetworkTransport
import com.itantra.app.data.database.iTantraDatabase
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.data.repository.MessageRepositoryImpl
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.model.PlaybackStatus
import com.itantra.app.domain.model.TransportType
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.domain.usecase.SynthesizeSpeechUseCase
import com.itantra.app.domain.usecase.TranscribeAudioUseCase
import com.itantra.app.domain.usecase.TransmitMessageUseCase
import com.itantra.app.speech.audio.PlaybackQueue
import com.itantra.app.speech.stt.OfflineSTTEngine
import com.itantra.app.speech.stt.TeluguOfflineSTTEngine
import com.itantra.app.speech.tts.OfflineTTSEngine
import com.itantra.app.speech.tts.TeluguOfflineTTSEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class iTantraApplication : Application() {

    val appScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    lateinit var appPreferences: AppPreferences
        private set
    lateinit var database: iTantraDatabase
        private set
    lateinit var messageRepository: MessageRepository
        private set

    lateinit var sttEngine: OfflineSTTEngine
        private set
    lateinit var ttsEngine: OfflineTTSEngine
        private set
    lateinit var playbackQueue: PlaybackQueue
        private set

    lateinit var mockTransport: MockLoopbackTransport
        private set
    lateinit var localNetworkTransport: LocalNetworkTransport
        private set
    lateinit var bluetoothTransport: BluetoothTransport
        private set

    lateinit var activeTransport: CommunicationTransport
        private set

    lateinit var transcribeAudioUseCase: TranscribeAudioUseCase
        private set
    lateinit var synthesizeSpeechUseCase: SynthesizeSpeechUseCase
        private set
    lateinit var transmitMessageUseCase: TransmitMessageUseCase
        private set
    lateinit var storeAndForwardQueue: StoreAndForwardQueue
        private set

    private val packetFragmenter = PacketFragmenter()

    companion object {
        private const val TAG = "iTantraApp"
    }

    override fun onCreate() {
        super.onCreate()
        TacticalLogger.i(TAG, "Initializing iTantra Tactical Node Runtime...")

        appPreferences = AppPreferences(this)
        database = iTantraDatabase.getInstance(this)
        messageRepository = MessageRepositoryImpl(database.messageDao())

        // AI Speech Engines
        sttEngine = TeluguOfflineSTTEngine()
        ttsEngine = TeluguOfflineTTSEngine(this)
        playbackQueue = PlaybackQueue(ttsEngine)

        // Pre-initialize Telugu engines
        appScope.launch {
            sttEngine.initialize(appPreferences.selectedLanguage)
            ttsEngine.initialize(appPreferences.selectedLanguage)
        }

        // Transports
        mockTransport = MockLoopbackTransport()
        localNetworkTransport = LocalNetworkTransport()
        bluetoothTransport = BluetoothTransport(this)

        activeTransport = when (appPreferences.selectedTransport) {
            TransportType.BLUETOOTH -> bluetoothTransport
            TransportType.LOCAL_NETWORK -> localNetworkTransport
            else -> mockTransport
        }

        // Use Cases
        transcribeAudioUseCase = TranscribeAudioUseCase(sttEngine)
        synthesizeSpeechUseCase = SynthesizeSpeechUseCase(ttsEngine)
        transmitMessageUseCase = TransmitMessageUseCase(messageRepository, activeTransport)

        // Store-and-forward queue daemon
        storeAndForwardQueue = StoreAndForwardQueue(
            messageRepository = messageRepository,
            transport = activeTransport,
            transmitMessageUseCase = transmitMessageUseCase
        )
        storeAndForwardQueue.start()

        // Start listening on active link for incoming transmissions
        observeIncomingPackets()
    }

    private val processedPacketKeys = java.util.Collections.synchronizedSet(LinkedHashSet<String>())

    private fun observeIncomingPackets() {
        appScope.launch {
            activeTransport.observeIncomingPackets().collect { rawPacket ->
                handleIncomingPacket(rawPacket)
            }
        }
    }

    private fun handleIncomingPacket(packet: iMFPPacket) {
        appScope.launch {
            try {
                // Safeguard 9: Drop self-transmitted loopback packets (Prevents sender echoing to its own speaker)
                val localNodeId = 0xA89F.toShort()
                if (packet.sourceNodeId == localNodeId) {
                    TacticalLogger.d(TAG, "Ignoring self-transmitted loopback frame from local node ($localNodeId)")
                    return@launch
                }

                // Safeguard 10: Sliding window packet deduplication (Prevents duplicate Bluetooth/Wi-Fi/LoRa retries)
                val packetKey = "${packet.sourceNodeId}_${packet.sequenceNumber}_${packet.payload.contentHashCode()}"
                synchronized(processedPacketKeys) {
                    if (processedPacketKeys.contains(packetKey)) {
                        TacticalLogger.w(TAG, "Duplicate packet detected and dropped: key=$packetKey")
                        return@launch
                    }
                    processedPacketKeys.add(packetKey)
                    if (processedPacketKeys.size > 200) {
                        val it = processedPacketKeys.iterator()
                        if (it.hasNext()) { it.next(); it.remove() }
                    }
                }

                // Safeguard 11: ACK packets should update delivery status, not create new messages
                if (packet.isAck) {
                    TacticalLogger.i(TAG, "Received ACK for seq=${packet.sequenceNumber} from node ${packet.sourceNodeId}")
                    return@launch
                }

                // 1. Decompress payload
                val decompressedText = TextCompressor.decompress(packet.payload, packet.compressionType)
                TacticalLogger.i(TAG, "[iTantra-Trace] MESSAGE_RECEIVED (seq=${packet.sequenceNumber}, src=Node ${packet.sourceNodeId}, lang=${packet.language.displayName}, text='$decompressedText')")

                // 2. Persist in local database as incoming message
                val incomingMsg = Message(
                    conversationId = "default_channel",
                    senderId = "Node ${packet.sourceNodeId}",
                    receiverId = appPreferences.nodeCallsign,
                    text = decompressedText,
                    language = packet.language,
                    priority = packet.priority,
                    deliveryStatus = DeliveryStatus.DELIVERED,
                    playbackStatus = PlaybackStatus.UNPLAYED,
                    isIncoming = true,
                    originalSizeBytes = decompressedText.toByteArray(Charsets.UTF_8).size,
                    compressedSizeBytes = packet.payload.size
                )
                messageRepository.insertMessage(incomingMsg)

                // 3. Audio synthesis on receiver (Layer 5) with strictly one playback session
                if (appPreferences.isAutoPlayTts) {
                    TacticalLogger.i(TAG, "[iTantra-Trace] TTS_STARTED (msgId=${incomingMsg.messageId}, text='${incomingMsg.text}')")
                    playbackQueue.enqueue(incomingMsg) { playedMsg ->
                        TacticalLogger.i(TAG, "[iTantra-Trace] TTS_COMPLETED (msgId=${playedMsg.messageId})")
                        appScope.launch {
                            messageRepository.updatePlaybackStatus(playedMsg.messageId, PlaybackStatus.PLAYED)
                        }
                    }
                }
            } catch (e: Exception) {
                TacticalLogger.e(TAG, "Error handling received packet", e)
            }
        }
    }
}
