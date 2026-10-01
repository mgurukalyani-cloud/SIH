package com.itantra.app.core.common

import java.util.UUID

object AppConstants {
    const val APP_NAME = "iTantra"
    const val PROTOCOL_VERSION = 1
    
    // Audio Sampling Configuration
    const val AUDIO_SAMPLE_RATE = 16000 // 16 kHz mono standard for Indic ASR
    const val AUDIO_CHANNELS = 1 // Mono
    const val AUDIO_FRAME_DURATION_MS = 30 // 30 ms frames
    const val SAMPLES_PER_FRAME = (AUDIO_SAMPLE_RATE * AUDIO_FRAME_DURATION_MS) / 1000 // 480 samples
    
    // VAD Configuration Defaults
    const val DEFAULT_VAD_ENERGY_THRESHOLD = 450.0
    const val DEFAULT_SILENCE_HANGOVER_MS = 600L
    const val MIN_VOICED_FRAMES_REQUIRED = 4 // Minimum 120ms of voice to avoid noise burst
    
    // Low Bitrate Framing & MTU
    const val DEFAULT_MTU_BYTES = 256 // Bluetooth / LoRa safe MTU
    const val IMFP_HEADER_SIZE = 14 // 14-byte compact binary header
    const val BROADCAST_NODE_ID: Short = 0xFFFF.toShort()
    
    // Default Ports & UUIDs
    val BLUETOOTH_SERVICE_UUID: UUID = UUID.fromString("0000FA10-0000-1000-8000-00805F9B34FB")
    const val DEFAULT_LOCAL_TCP_PORT = 8766
    
    // Confidence Thresholds
    const val CONFIDENCE_REVIEW_THRESHOLD = 0.75f // Trigger confirmation dialog if below 75%
}
