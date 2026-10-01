package com.itantra.app.modelmanagement.validation

import android.os.Environment
import android.os.StatFs
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.modelmanagement.registry.ModelMetadata
import com.itantra.app.modelmanagement.registry.ModelRegistry
import com.itantra.app.modelmanagement.registry.ModelStatus

data class ValidationReport(
    val language: AppLanguage,
    val isReadyForInference: Boolean,
    val userStatusMessage: String,
    val availableDiskSpaceMB: Long,
    val requiredDiskSpaceMB: Long
)

object ModelValidator {

    fun validate(language: AppLanguage): ValidationReport {
        val metadata = ModelRegistry.getMetadata(language)
        val availableBytes = getAvailableInternalMemorySize()
        val availableMB = availableBytes / (1024 * 1024)
        val requiredMB = metadata.modelSizeBytes / (1024 * 1024)

        return when (metadata.status) {
            ModelStatus.VALIDATED_AND_READY -> {
                ValidationReport(
                    language = language,
                    isReadyForInference = true,
                    userStatusMessage = "Offline AI Ready (Telugu Primary Model Validated)",
                    availableDiskSpaceMB = availableMB,
                    requiredDiskSpaceMB = requiredMB
                )
            }
            ModelStatus.DEMO_READY -> {
                ValidationReport(
                    language = language,
                    isReadyForInference = true,
                    userStatusMessage = "Demo Ready (${metadata.language.displayName})",
                    availableDiskSpaceMB = availableMB,
                    requiredDiskSpaceMB = requiredMB
                )
            }
            ModelStatus.NOT_INSTALLED -> {
                ValidationReport(
                    language = language,
                    isReadyForInference = false,
                    userStatusMessage = "Model not installed or not validated for this device.",
                    availableDiskSpaceMB = availableMB,
                    requiredDiskSpaceMB = requiredMB
                )
            }
        }
    }

    private fun getAvailableInternalMemorySize(): Long {
        return try {
            val path = Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            availableBlocks * blockSize
        } catch (e: Exception) {
            500L * 1024 * 1024 // 500 MB fallback
        }
    }
}
