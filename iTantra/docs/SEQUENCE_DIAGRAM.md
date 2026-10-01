# iTantra — End-to-End Sequence Diagram

The complete transmission pipeline: User Speech $\rightarrow$ Offline STT $\rightarrow$ Compact Text $\rightarrow$ Low-Bitrate Radio Link $\rightarrow$ Offline TTS $\rightarrow$ Receiver Speech.

```mermaid
sequenceDiagram
    autonumber
    actor Sender as Node Alpha (Field Operator)
    participant Mic as AudioRecord (16kHz PCM)
    participant VAD as VoiceActivityDetector
    participant STT as TeluguOfflineSTTEngine
    participant Codec as iMFPPacket & Compressor
    participant Radio as CommunicationTransport
    participant ReceiverNode as Node Bravo (SAR Basecamp)
    participant ReceiverDB as Room Database
    participant TTS as TeluguOfflineTTSEngine
    participant Speaker as AudioPlayer / Speaker

    Note over Sender, Mic: Phase 1: Local Voice Capture & VAD
    Sender->>Mic: Press & Hold PTT Button
    Mic->>VAD: Stream 30ms PCM Frames (480 samples)
    VAD->>VAD: Compute RMS Energy & ZCR
    Sender->>Mic: Release PTT Button (Utterance End)
    Mic->>VAD: Check Minimum Voiced Frames (>= 120ms)

    Note over VAD, STT: Phase 2: Offline On-Device ASR
    VAD->>STT: Handover Filtered Audio Buffer
    STT->>STT: INT8 Conformer-CTC Inference (RTF ~ 0.22)
    STT-->>Sender: Return Decoded Telugu Text & Confidence Score (94%)

    Note over Sender, Codec: Phase 3: Binary Framing & Checksum
    Sender->>Codec: Confirm & Transmit ("వరద నీరు పెరుగుతోంది, సహాయం కావాలి")
    Codec->>Codec: Deflate Compress / UTF-8 Check
    Codec->>Codec: Pack into 14-byte iMFP Frame + CRC-16 Checksum

    Note over Codec, Radio: Phase 4: Over-the-Air Transmission
    Codec->>Radio: Dispatch Binary Frame (42 Bytes total)
    Radio->>ReceiverNode: Transmit across Bluetooth / Wi-Fi Direct / LoRa RF

    Note over ReceiverNode, ReceiverDB: Phase 5: Frame Validation & Persistence
    ReceiverNode->>ReceiverNode: Validate Magic Bytes (0x7E 0x1A)
    ReceiverNode->>ReceiverNode: Calculate & Verify CRC-16 Checksum
    ReceiverNode->>ReceiverDB: Persist Message (Status: DELIVERED)
    ReceiverNode-->>Radio: Return Delivery ACK Frame

    Note over ReceiverNode, Speaker: Phase 6: Offline Speech Synthesis
    ReceiverNode->>TTS: Request Speech Synthesis (Telugu Locale)
    TTS->>TTS: Indic VITS Neural Vocoder / System TTS
    TTS->>Speaker: Stream Synthesized 16kHz PCM Audio
    Speaker-->>ReceiverNode: Audible Telugu Voice Output!
```
