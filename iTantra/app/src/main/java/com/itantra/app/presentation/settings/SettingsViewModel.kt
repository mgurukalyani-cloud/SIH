package com.itantra.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.modelmanagement.registry.ModelRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedLanguage: AppLanguage = AppLanguage.TELUGU,
    val vadThreshold: Float = 450f,
    val isAutoPlayTts: Boolean = true,
    val isCompressionEnabled: Boolean = true,
    val nodeCallsign: String = "Node Alpha",
    val clearSuccess: Boolean = false
)

class SettingsViewModel(
    private val prefs: AppPreferences,
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            selectedLanguage = prefs.selectedLanguage,
            vadThreshold = prefs.vadThreshold,
            isAutoPlayTts = prefs.isAutoPlayTts,
            isCompressionEnabled = prefs.isCompressionEnabled,
            nodeCallsign = prefs.nodeCallsign
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun updateLanguage(lang: AppLanguage) {
        if (ModelRegistry.isLanguageAvailable(lang)) {
            prefs.selectedLanguage = lang
            _uiState.value = _uiState.value.copy(selectedLanguage = lang)
        }
    }

    fun updateVadThreshold(value: Float) {
        prefs.vadThreshold = value
        _uiState.value = _uiState.value.copy(vadThreshold = value)
    }

    fun toggleAutoPlay(enabled: Boolean) {
        prefs.isAutoPlayTts = enabled
        _uiState.value = _uiState.value.copy(isAutoPlayTts = enabled)
    }

    fun toggleCompression(enabled: Boolean) {
        prefs.isCompressionEnabled = enabled
        _uiState.value = _uiState.value.copy(isCompressionEnabled = enabled)
    }

    fun updateCallsign(name: String) {
        prefs.nodeCallsign = name
        _uiState.value = _uiState.value.copy(nodeCallsign = name)
    }

    fun clearMessageHistory() {
        viewModelScope.launch {
            messageRepository.clearAllMessages()
            _uiState.value = _uiState.value.copy(clearSuccess = true)
        }
    }
}
