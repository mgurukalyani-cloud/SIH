package com.itantra.app.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.domain.repository.MessageRepository
import com.itantra.app.modelmanagement.validation.ModelValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val callsign: String = "Node Alpha",
    val deviceId: String = "0x0001",
    val currentLanguage: AppLanguage = AppLanguage.TELUGU,
    val aiStatusLabel: String = "Offline AI Ready",
    val isAiReady: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val connectedPeerName: String? = null,
    val pendingCount: Int = 0,
    val lastMessageTime: String? = null,
    val isEmergencyActive: Boolean = false
)

class DashboardViewModel(
    private val prefs: AppPreferences,
    private val messageRepository: MessageRepository,
    private val transport: CommunicationTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val messages = messageRepository.getAllMessages().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val pendingQueue = messageRepository.getPendingQueue().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        refreshState()
        observeTransport()
        observeQueue()
    }

    fun refreshState() {
        val lang = prefs.selectedLanguage
        val validation = ModelValidator.validate(lang)

        _uiState.value = _uiState.value.copy(
            callsign = prefs.nodeCallsign,
            deviceId = "0x${prefs.deviceId}",
            currentLanguage = lang,
            aiStatusLabel = validation.userStatusMessage,
            isAiReady = validation.isReadyForInference
        )
    }

    private fun observeTransport() {
        viewModelScope.launch {
            transport.connectionState().collect { state ->
                val peerName = if (state is ConnectionState.Connected) state.device.name else null
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    connectedPeerName = peerName
                )
            }
        }
    }

    private fun observeQueue() {
        viewModelScope.launch {
            messageRepository.getPendingQueue().collect { pendingList ->
                _uiState.value = _uiState.value.copy(
                    pendingCount = pendingList.size
                )
            }
        }
    }

    fun triggerEmergencySOS(onSosDispatched: (Message) -> Unit) {
        viewModelScope.launch {
            val sosMsg = Message(
                senderId = prefs.nodeCallsign,
                text = "🚨 తక్షణ రక్షణ అవసరం! ఎమర్జెన్సీ SOS పంపబడింది (EMERGENCY SOS)",
                language = prefs.selectedLanguage,
                priority = com.itantra.app.domain.model.MessagePriority.EMERGENCY,
                deliveryStatus = DeliveryStatus.QUEUED
            )
            onSosDispatched(sosMsg)
        }
    }
}
