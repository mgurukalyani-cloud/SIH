# Smart India Hackathon (SIH) — iTantra

> **Offline Multilingual Voice Communication Pipeline for Constrained & Low-Bitrate Disaster Channels**

[![Platform](https://img.shields.io/badge/Platform-Android_Native_%7C_Web_Mesh-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Language](https://img.shields.io/badge/Kotlin-2.0.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/Jetpack_Compose-Material3-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Savings](https://img.shields.io/badge/Bandwidth_Savings-99.8%25-10B981.svg)]()
[![Offline](https://img.shields.io/badge/Cloud_Dependency-ZERO_(100%25_Offline)-critical.svg)]()

---

## 📁 Repository Structure

This repository contains the complete solution for **iTantra**, organized into two main project directories:

```
SIH/
├── iTantra/                 # Native Android Application (Production Core)
│   ├── app/                 # Clean Architecture source code (Compose, Room, VAD, STT, TTS, Transports)
│   ├── docs/                # Architecture diagrams, benchmark reports, and protocol specifications
│   ├── gradle/              # Version catalog and build configurations
│   ├── build.gradle.kts     # Project Gradle build script
│   └── README.md            # Detailed Android project guide & documentation
│
├── itantra-web/             # Interactive Dual-Phone Web Simulator & Telemetry Console
│   ├── index.html           # Split-screen Phone A (Tx) & Phone B (Rx) live demonstrator
│   ├── app.js               # Pipeline state machine, Web Speech STT/TTS, and BroadcastChannel mesh
│   ├── server.js            # Zero-dependency local Node.js server for testing
│   ├── style.css            # Tactical disaster-response HUD styling
│   └── README.md            # Web demonstrator instructions & feature guide
│
├── .gitignore               # Clean git exclusion rules
└── README.md                # Repository overview (this file)
```

---

## 🎯 The Core Problem & Solution

In disaster zones (earthquakes, cyclones, floods, or collapsed infrastructure), cellular networks and internet connectivity fail completely. While high-bandwidth audio streaming (Opus, AMR-WB requiring 6,000–24,000 bps) collapses under constrained radio links (VHF, HF, LoRa, or BLE at ≤ 1,200 bps), **iTantra replaces audio streaming with semantic text transmission**:

$$\text{Speech} \xrightarrow{\text{Offline STT}} \text{Text} \xrightarrow{\text{Compression / iMFP}} \left[\begin{array}{c}\text{Bluetooth RFCOMM}\\\text{Wi-Fi Direct}\\\text{LoRa Mesh}\end{array}\right] \xrightarrow{\text{Decompress}} \text{Text} \xrightarrow{\text{Offline TTS}} \text{Speech}$$

* **Raw Audio (3-second message):** ~96,000 Bytes
* **iTantra Transmitted Frame:** ~42–54 Bytes
* **Bandwidth Reduction:** **99.9% data reduction** (over 1,700× less data)
* **Air-time at 1.2 kbps:** Reduced from **640 seconds (unusable)** to **0.36 seconds (instant)**

---

## 🚀 Projects Overview

### 1. [`iTantra/`](./iTantra/) — Native Android Terminal
* **Jetpack Compose UI**: 8 complete screens including Welcome, Setup, Dashboard, Push-To-Talk (PTT), Conversation, Diagnostics, Settings, and Peer Connection.
* **100% Offline AI Engines**: On-device Indic STT and TTS supporting 10 Indian languages (Telugu, Hindi, Tamil, Kannada, Malayalam, Marathi, Gujarati, Odia, Bengali, English).
* **Ad-Hoc P2P Transports**: Bluetooth Classic RFCOMM, Local Wi-Fi Direct socket server, and Store-and-Forward offline queue.
* **Concurrency Safeguards**: Atomic state locks (`isRecordingActive`, `isSttActive`, `isTransmittingActive`), sliding-window packet deduplication, self-loopback filtering, and acoustic feedback loop prevention.

### 2. [`itantra-web/`](./itantra-web/) — Dual-Phone Web Simulator
* **Two-Panel Simulator**: Live split-screen displaying **Phone A (Transmitter)** and **Phone B (Receiver)**.
* **Multi-Device Testing**: Open `/?mode=tx` on one phone and `/?mode=rx` on another phone over local Wi-Fi to test cross-device communication in real-time.
* **10-Stage Pipeline Telemetry**: Animated visualization tracking audio ingress, local STT, iMFP framing, constrained link transfer, and speaker playback.
* **Emergency Broadcast System**: Non-interruptible SOS broadcast with maximum volume alert override.

---

## 🛠️ Quick Start

### Android Application
1. Open the [`iTantra/`](./iTantra/) folder in **Android Studio Hedgehog / Ladybug** or newer.
2. Ensure Android SDK 34 is installed.
3. Sync Gradle and build the project (`./gradlew assembleDebug`).
4. Deploy to two physical Android devices to test direct Bluetooth or Wi-Fi Direct communication.

### Web Simulator
1. Navigate to the [`itantra-web/`](./itantra-web/) directory.
2. Start the local server:
   ```bash
   node server.js
   ```
3. Open `http://localhost:8765` in your browser.
4. For dual-phone testing on the same Wi-Fi:
   * **Phone 1 (Sender):** `http://<YOUR_IP>:8765/?mode=tx`
   * **Phone 2 (Receiver):** `http://<YOUR_IP>:8765/?mode=rx`
