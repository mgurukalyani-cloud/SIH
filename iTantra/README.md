# iTantra — Offline Multilingual Voice Communication for Low-Bitrate Links

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%206--Layer%20Modular-00B4D8.svg)]()
[![Bandwidth Savings](https://img.shields.io/badge/Bandwidth%20Reduction-99.8%25-10B981.svg)]()

> **An offline tactical communication terminal engineered for disaster-response teams, remote field workers, emergency operators, and constrained low-bitrate links (Bluetooth, Wi-Fi Direct, HF/VHF tactical radio, LoRa, and satellite links).**

---

## 🚀 The Core Problem & Philosophy

Traditional digital voice communication streams continuous compressed acoustic waveforms (AMR-WB, Opus), requiring **6,000 to 24,000 bits per second (bps)**. In disaster zones (cyclones, earthquakes, landslides) or underground tunnels, communication channels suffer severe attenuation, path loss, and congestion, dropping throughput below 1,000 bps where raw voice audio mutters, stutters, and disconnects completely.

**iTantra replaces audio streaming with semantic text transmission:**

```
User Telugu Speech
       │
       ▼
[16 kHz PCM Audio Capture]
       │
       ▼
[Energy RMS & ZCR Voice Activity Detection]
       │
       ▼
[Offline Telugu Indic Speech-to-Text (STT)]
       │
       ▼
[Sentence Formation & Confidence Validation]
       │
       ▼
[iMFP 14-Byte Compact Framing + Deflate Compression + CRC-16]
       │
       ▼
[Constrained Radio Link: Bluetooth / Wi-Fi Direct / LoRa / Tactical Radio]
       │
       ▼
[Receiver Integrity Check & Defragmentation]
       │
       ▼
[Local Room Storage (Store-and-Forward / Delivery State)]
       │
       ▼
[Offline Telugu Indic Text-to-Speech (TTS)]
       │
       ▼
Receiver Speaker / Audible Playback
```

Instead of sending 96,000 bytes for a 3-second voice recording, iTantra transmits an **ultra-compact ~42-byte binary frame** — achieving a **99.9% bandwidth reduction** while preserving full human voice semantics.

---

## 📱 8 Jetpack Compose Screens

1. **Welcome Screen (`Screen.Welcome`)**: Tactical branding, 100% offline AI badge, and explicit link limitation notice.
2. **Initial Setup Screen (`Screen.InitialSetup`)**: Language picker (Telugu highlighted as primary validated MVP), model presence verifier, mic & speaker hardware check, and callsign config.
3. **Main Dashboard (`Screen.Dashboard`)**: Real-time status cards (AI Pipeline, Transport Link, Store-and-Forward Queue counter, battery level, big PTT trigger, and Emergency SOS broadcast).
4. **Push-to-Talk Screen (`Screen.PushToTalk`)**: Hold-to-record tactical interface, real-time waveform visualizer, live VAD state badge (`SILENCE`, `SPEECH_STARTED`, `VOICE_ACTIVE`, `UTTERANCE_COMPLETE`).
5. **Message Confirmation Card**: Transcribed text preview, payload byte size counter, transmission air time calculator (at 1200 bps LoRa), and low-confidence review modal dialog.
6. **Conversation Screen (`Screen.Conversation`)**: Tactical chat stream with sent/received bubbles, delivery state markers (`QUEUED`, `SENDING`, `SENT`, `DELIVERED`, `PENDING`), one-tap TTS audio replay, and text fallback input.
7. **Device Connection Screen (`Screen.DeviceConnection`)**: Dynamic transport switcher (Bluetooth RFCOMM SPP, Local Wi-Fi / Hotspot TCP Socket, Mock Loopback), peer discovery list, signal RSSI, and manual IP/port connection.
8. **Settings & Diagnostics Screen (`Screen.Settings` / `Screen.Diagnostics`)**: VAD sensitivity slider (200–1000 RMS), adaptive compression toggles, 10-language model registry, real-time audit logs, and performance benchmark suite.

---

## 🇮🇳 10 Target Indian Languages

| Language | Code | Native Name | Status | Model Engine |
|---|---|---|---|---|
| **Telugu** | `te` | **తెలుగు** | **✓ Validated Primary MVP** | IndicASR Conformer INT8 / IndicTTS |
| **English** | `en` | **English** | **✓ Demo Ready** | Conformer-Tiny INT8 / System TTS |
| Hindi | `hi` | हिन्दी | Model Not Installed | Gated in ModelRegistry |
| Gujarati | `gu` | ગુજરાતી | Model Not Installed | Gated in ModelRegistry |
| Marathi | `mr` | मराठी | Model Not Installed | Gated in ModelRegistry |
| Kannada | `kn` | ಕನ್ನಡ | Model Not Installed | Gated in ModelRegistry |
| Malayalam | `ml` | മലയാളം | Model Not Installed | Gated in ModelRegistry |
| Tamil | `ta` | தமிழ் | Model Not Installed | Gated in ModelRegistry |
| Odia | `or` | ଓଡ଼ିଆ | Model Not Installed | Gated in ModelRegistry |
| Bengali | `bn` | বাংলা | Model Not Installed | Gated in ModelRegistry |

> **Development Rule:** Per project requirements, languages without verified on-device INT8 weights display: *"Model not installed or not validated for this device."*

---

## 📦 iMFP Binary Protocol (14-Byte Header)

```
[0..1]   : Magic Delimiter (0x7E 0x1A)
[2]      : Version (3 bits) | Priority (2 bits) | Compression (2 bits) | IsAck (1 bit)
[3]      : Language ID (4 bits: Telugu=7, Hindi=1, etc.) | TTL (4 bits)
[4..5]   : Sequence Counter (16 bits)
[6..7]   : Source Node ID (16 bits)
[8..9]   : Destination Node ID (16 bits, 0xFFFF = Broadcast)
[10..11] : Payload Length M in bytes (16 bits)
[12..13] : CRC-16-CCITT Checksum (16 bits calculated over bytes 2..11 + Payload)
[14..14+M]: Compact Text Payload (UTF-8 or Deflate compressed)
```

---

## 🛠️ Building & Running in Android Studio

### Prerequisites
- **Android Studio** Iguana / Jellyfish / Koala (2024.1+)
- **Android SDK** API 34 (compileSdk 34, minSdk 26)
- **JDK** 17 or 21

### Steps
1. Open Android Studio.
2. Select **Open** $\rightarrow$ Navigate to `C:\Users\Admin\.gemini\antigravity\scratch\iTantra`.
3. Allow Gradle to sync dependencies (`Jetpack Compose`, `Room`, `Kotlin Coroutines`).
4. Select an emulator or physical device running Android 8.0+ (API 26+).
5. Click **Run 'app'** (`Shift + F10`).

---

## 🧪 Running Automated Unit Tests

Run unit tests via command line or Android Studio:
```bash
./gradlew test
```
The test suite covers:
- `iMFPPacketTest`: Binary serialization, magic delimiter, and CRC-16 bit-corruption rejection.
- `PacketFragmenterTest`: MTU segmentation, reassembly, and out-of-order packet recovery.
- `TextCompressorTest`: Deflate vs UTF-8 benchmark and short message expansion protection.
- `VoiceActivityDetectorTest`: RMS energy calculation, silence gating, and speech state transitions.
- `ModelRegistryTest`: Primary MVP validation checks and unsupported language gating.
