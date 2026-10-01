package com.itantra.app.domain.model

enum class AppLanguage(
    val id: Int,
    val code: String,
    val displayName: String,
    val nativeName: String,
    val isValidated: Boolean = false
) {
    HINDI(1, "hi", "Hindi", "हिन्दी", false),
    GUJARATI(2, "gu", "Gujarati", "ગુજરાતી", false),
    MARATHI(3, "mr", "Marathi", "मराठी", false),
    KANNADA(4, "kn", "Kannada", "ಕನ್ನಡ", false),
    MALAYALAM(5, "ml", "Malayalam", "മലയാളം", false),
    TAMIL(6, "ta", "Tamil", "தமிழ்", false),
    TELUGU(7, "te", "Telugu", "తెలుగు", true), // Validated Primary MVP
    ODIA(8, "or", "Odia", "ଓଡ଼ିଆ", false),
    BENGALI(9, "bn", "Bengali", "বাংলা", false),
    ENGLISH(10, "en", "English", "English", true);

    companion object {
        fun fromId(id: Int): AppLanguage {
            return entries.firstOrNull { it.id == id } ?: TELUGU
        }

        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: TELUGU
        }
    }
}
