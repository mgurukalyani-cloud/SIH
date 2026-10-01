package com.itantra.app.modelmanagement

import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.modelmanagement.registry.ModelRegistry
import com.itantra.app.modelmanagement.registry.ModelStatus
import com.itantra.app.modelmanagement.validation.ModelValidator
import org.junit.Assert.*
import org.junit.Test

class ModelRegistryTest {

    @Test
    fun testTeluguIsValidatedPrimaryMVP() {
        val meta = ModelRegistry.getMetadata(AppLanguage.TELUGU)
        assertEquals(ModelStatus.VALIDATED_AND_READY, meta.status)
        assertTrue(ModelRegistry.isLanguageAvailable(AppLanguage.TELUGU))

        val validation = ModelValidator.validate(AppLanguage.TELUGU)
        assertTrue(validation.isReadyForInference)
    }

    @Test
    fun testUnvalidatedLanguagesAreGated() {
        val unvalidatedLanguages = listOf(
            AppLanguage.HINDI,
            AppLanguage.GUJARATI,
            AppLanguage.MARATHI,
            AppLanguage.KANNADA,
            AppLanguage.MALAYALAM,
            AppLanguage.TAMIL,
            AppLanguage.ODIA,
            AppLanguage.BENGALI
        )

        for (lang in unvalidatedLanguages) {
            val meta = ModelRegistry.getMetadata(lang)
            assertEquals(ModelStatus.NOT_INSTALLED, meta.status)
            assertFalse(ModelRegistry.isLanguageAvailable(lang))

            val validation = ModelValidator.validate(lang)
            assertFalse(validation.isReadyForInference)
            assertEquals("Model not installed or not validated for this device.", validation.userStatusMessage)
        }
    }

    @Test
    fun testAllTenIndianLanguagesArePresentInCatalog() {
        assertEquals(10, AppLanguage.entries.size)
        for (lang in AppLanguage.entries) {
            val meta = ModelRegistry.getMetadata(lang)
            assertNotNull(meta)
            assertTrue(meta.modelSizeBytes > 0)
        }
    }
}
