package com.itantra.app.modelmanagement.registry

import com.itantra.app.domain.model.AppLanguage

enum class ModelStatus {
    VALIDATED_AND_READY,
    DEMO_READY,
    NOT_INSTALLED
}

data class ModelMetadata(
    val language: AppLanguage,
    val sttModelName: String,
    val sttModelPath: String,
    val ttsModelName: String,
    val ttsModelPath: String,
    val supportedSampleRate: Int = 16000,
    val tokenizerConfig: String,
    val status: ModelStatus,
    val version: String,
    val license: String,
    val modelSizeBytes: Long
)
