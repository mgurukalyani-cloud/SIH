package com.itantra.app.modelmanagement.registry

import com.itantra.app.domain.model.AppLanguage

object ModelRegistry {

    private val catalog: Map<AppLanguage, ModelMetadata> = mapOf(
        AppLanguage.TELUGU to ModelMetadata(
            language = AppLanguage.TELUGU,
            sttModelName = "AI4Bharat IndicASR Telugu Conformer-CTC INT8",
            sttModelPath = "models/te/conformer_ctc_int8.onnx",
            ttsModelName = "AI4Bharat IndicTTS Telugu VITS",
            ttsModelPath = "models/te/vits_telugu.onnx",
            tokenizerConfig = "BPE 4096 tokens (Kathbath Indic)",
            status = ModelStatus.VALIDATED_AND_READY,
            version = "v1.2.0-int8",
            license = "CC-BY-4.0 / MIT Indic-Speech",
            modelSizeBytes = 52_428_800L // 52 MB
        ),
        AppLanguage.ENGLISH to ModelMetadata(
            language = AppLanguage.ENGLISH,
            sttModelName = "Conformer-Tiny English INT8",
            sttModelPath = "models/en/conformer_tiny_en.onnx",
            ttsModelName = "Android System English TTS",
            ttsModelPath = "system/tts/en_IN",
            tokenizerConfig = "Character / BPE 1024",
            status = ModelStatus.DEMO_READY,
            version = "v1.0.0",
            license = "Apache 2.0",
            modelSizeBytes = 38_000_000L
        ),
        AppLanguage.HINDI to ModelMetadata(
            language = AppLanguage.HINDI,
            sttModelName = "IndicASR Hindi INT8",
            sttModelPath = "models/hi/conformer_hi.onnx",
            ttsModelName = "IndicTTS Hindi VITS",
            ttsModelPath = "models/hi/vits_hi.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.3",
            license = "CC-BY-4.0",
            modelSizeBytes = 54_000_000L
        ),
        AppLanguage.GUJARATI to ModelMetadata(
            language = AppLanguage.GUJARATI,
            sttModelName = "IndicASR Gujarati INT8",
            sttModelPath = "models/gu/conformer_gu.onnx",
            ttsModelName = "IndicTTS Gujarati",
            ttsModelPath = "models/gu/vits_gu.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 51_000_000L
        ),
        AppLanguage.MARATHI to ModelMetadata(
            language = AppLanguage.MARATHI,
            sttModelName = "IndicASR Marathi INT8",
            sttModelPath = "models/mr/conformer_mr.onnx",
            ttsModelName = "IndicTTS Marathi",
            ttsModelPath = "models/mr/vits_mr.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 53_000_000L
        ),
        AppLanguage.KANNADA to ModelMetadata(
            language = AppLanguage.KANNADA,
            sttModelName = "IndicASR Kannada INT8",
            sttModelPath = "models/kn/conformer_kn.onnx",
            ttsModelName = "IndicTTS Kannada",
            ttsModelPath = "models/kn/vits_kn.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 52_000_000L
        ),
        AppLanguage.MALAYALAM to ModelMetadata(
            language = AppLanguage.MALAYALAM,
            sttModelName = "IndicASR Malayalam INT8",
            sttModelPath = "models/ml/conformer_ml.onnx",
            ttsModelName = "IndicTTS Malayalam",
            ttsModelPath = "models/ml/vits_ml.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 54_000_000L
        ),
        AppLanguage.TAMIL to ModelMetadata(
            language = AppLanguage.TAMIL,
            sttModelName = "IndicASR Tamil INT8",
            sttModelPath = "models/ta/conformer_ta.onnx",
            ttsModelName = "IndicTTS Tamil",
            ttsModelPath = "models/ta/vits_ta.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 53_500_000L
        ),
        AppLanguage.ODIA to ModelMetadata(
            language = AppLanguage.ODIA,
            sttModelName = "IndicASR Odia INT8",
            sttModelPath = "models/or/conformer_or.onnx",
            ttsModelName = "IndicTTS Odia",
            ttsModelPath = "models/or/vits_or.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 50_000_000L
        ),
        AppLanguage.BENGALI to ModelMetadata(
            language = AppLanguage.BENGALI,
            sttModelName = "IndicASR Bengali INT8",
            sttModelPath = "models/bn/conformer_bn.onnx",
            ttsModelName = "IndicTTS Bengali",
            ttsModelPath = "models/bn/vits_bn.onnx",
            tokenizerConfig = "SentencePiece Indic",
            status = ModelStatus.NOT_INSTALLED,
            version = "Planned v1.4",
            license = "CC-BY-4.0",
            modelSizeBytes = 55_000_000L
        )
    )

    fun getMetadata(language: AppLanguage): ModelMetadata {
        return catalog[language] ?: catalog[AppLanguage.TELUGU]!!
    }

    fun getAllModels(): List<ModelMetadata> = catalog.values.toList()

    fun isLanguageAvailable(language: AppLanguage): Boolean {
        val meta = getMetadata(language)
        return meta.status == ModelStatus.VALIDATED_AND_READY || meta.status == ModelStatus.DEMO_READY
    }
}
