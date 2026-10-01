# iTantra — 6-Layer Modular Architecture

iTantra is designed strictly with Clean Architecture and separation of concerns across 6 distinct layers. No layer directly couples speech processing with communication hardware.

---

## 🏗️ Architectural Diagram

```mermaid
flowchart TD
    subgraph L1["Layer 1: User Interface (Jetpack Compose)"]
        UI_Dash["DashboardScreen & Cards"]
        UI_PTT["PushToTalkScreen & Waveform"]
        UI_Confirm["MessageConfirmation & Review"]
        UI_Chat["ConversationScreen (Chat Stream)"]
        UI_Conn["DeviceConnectionScreen"]
        UI_Diag["DiagnosticsScreen & Logs"]
    end

    subgraph L2["Layer 2: Speech Processing"]
        AudioRec["AudioRecorder (16 kHz PCM)"]
        VAD["VoiceActivityDetector (RMS + ZCR)"]
        STT_Interface["OfflineSTTEngine Interface"]
        STT_Telugu["TeluguOfflineSTTEngine"]
        STT_Sherpa["SherpaOnnxSTTAdapter (INT8)"]
        Confidence["Confidence & Sentence Validator"]
    end

    subgraph L3["Layer 3: Message Processing"]
        Compressor["TextCompressor (Deflate / UTF-8)"]
        Fragmenter["PacketFragmenter (MTU 256B)"]
        PacketCodec["iMFPPacket (14B Header + CRC-16)"]
        QueueMgr["StoreAndForwardQueue (Daemon)"]
    end

    subgraph L4["Layer 4: Communication Abstraction"]
        TransportInterface["CommunicationTransport Interface"]
        BT_Transport["BluetoothTransport (RFCOMM SPP)"]
        LAN_Transport["LocalNetworkTransport (TCP Socket)"]
        Mock_Transport["MockLoopbackTransport (In-Memory)"]
        Future_Transports["FutureRadioTransport / Satellite"]
    end

    subgraph L5["Layer 5: Receiver Processing"]
        CRC_Check["CRC-16 Integrity Check"]
        Reassembly["Fragment Reassembly"]
        TTS_Interface["OfflineTTSEngine Interface"]
        TTS_Telugu["TeluguOfflineTTSEngine"]
        TTS_Sys["AndroidSystemTTSEngine (te_IN)"]
        PlayQueue["PlaybackQueue & AudioPlayer"]
    end

    subgraph L6["Layer 6: Local Persistence"]
        RoomDB["iTantraDatabase (Room / SQLite)"]
        MsgDAO["MessageDao (FIFO Queue & Delivery State)"]
        Prefs["AppPreferences (DataStore)"]
    end

    UI_PTT --> AudioRec --> VAD --> STT_Interface
    STT_Interface --> STT_Telugu --> STT_Sherpa
    STT_Telugu --> Confidence --> UI_Confirm
    UI_Confirm --> L3
    Compressor --> PacketCodec --> Fragmenter --> TransportInterface
    QueueMgr <--> MsgDAO

    TransportInterface --> BT_Transport
    TransportInterface --> LAN_Transport
    TransportInterface --> Mock_Transport
    TransportInterface --> Future_Transports

    TransportInterface --> CRC_Check --> Reassembly --> MsgDAO
    Reassembly --> TTS_Interface --> TTS_Telugu --> TTS_Sys --> PlayQueue
    PlayQueue --> UI_Chat
    RoomDB --> MsgDAO --> UI_Chat
```

---

## Layer Responsibilities

### Layer 1: User Interface
- Lightweight, high-contrast Jetpack Compose interface with Material 3.
- Large tactile PTT button with real-time waveform feedback.
- Low-confidence review dialog safeguarding against unintelligible voice transmissions.

### Layer 2: Speech Processing
- **Audio Capture**: `AudioRecord` configured for 16,000 Hz, 16-bit linear PCM, mono channel in 30 ms chunks (480 samples).
- **VAD Gating**: Root-mean-square energy:
  $$E = \sqrt{\frac{1}{N}\sum_{n=0}^{N-1} x[n]^2}$$
  Silence threshold ($E < 450.0$) with a 600 ms hangover timer prevents cutting off the final words of a sentence.
- **Indic ASR Engine**: INT8 quantized Conformer-CTC model with CTC greedy decoder and Telugu language dictionary.

### Layer 3: Message Processing
- **iMFP Binary Framing**: 14-byte compact header containing version, priority flags, language ID, sequence counter, node hashes, payload size, and CRC-16.
- **Adaptive Compression**: Analyzes payload size; messages under 35 bytes bypass compression to avoid dictionary overhead, while larger messages are compressed using Deflate.
- **Fragmentation**: Slices payloads exceeding link MTU (256 bytes) with fragment headers and handles out-of-order reassembly.

### Layer 4: Communication Abstraction
- Completely decouples the AI and message layers from physical radio hardware.
- Replaceable transport classes implementing `CommunicationTransport`:
  - `BluetoothTransport` (Android Bluetooth RFCOMM SPP)
  - `LocalNetworkTransport` (Wi-Fi Direct / Local Hotspot TCP Sockets)
  - `MockLoopbackTransport` (In-memory loopback for instant unit and single-device verification)

### Layer 5: Receiver Processing
- Frame validation with CRC-16 checksum.
- Synthesizes received text using local Indic TTS (VITS / Android system offline voice).
- Sequential `PlaybackQueue` with emergency preemption.

### Layer 6: Local Persistence
- Room Database (`iTantraDatabase`) tracking full message lifecycle: `DRAFT` $\rightarrow$ `QUEUED` $\rightarrow$ `SENDING` $\rightarrow$ `SENT` $\rightarrow$ `DELIVERED` or `PENDING` / `FAILED`.
