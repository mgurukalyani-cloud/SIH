package com.itantra.app.presentation.connection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConnectionUiState(
    val activeTransport: TransportType = TransportType.MOCK,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val discoveredDevices: List<RemoteDevice> = emptyList(),
    val isScanning: Boolean = false,
    val ipInput: String = "192.168.43.1",
    val portInput: String = "8766",
    val errorMessage: String? = null
)

class ConnectionViewModel(
    private val prefs: AppPreferences,
    private val transport: CommunicationTransport
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ConnectionUiState(activeTransport = prefs.selectedTransport)
    )
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        observeConnection()
        startScan()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            transport.connectionState().collect { state ->
                _uiState.value = _uiState.value.copy(
                    connectionState = state,
                    errorMessage = if (state is ConnectionState.Error) state.message else null
                )
            }
        }
    }

    fun setTransport(type: TransportType) {
        prefs.selectedTransport = type
        _uiState.value = _uiState.value.copy(activeTransport = type)
        startScan()
    }

    fun startScan() {
        _uiState.value = _uiState.value.copy(isScanning = true, errorMessage = null)
        viewModelScope.launch {
            try {
                transport.startDiscovery().collect { devices ->
                    _uiState.value = _uiState.value.copy(
                        discoveredDevices = devices,
                        isScanning = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    errorMessage = e.localizedMessage
                )
            }
        }
    }

    fun connect(device: RemoteDevice) {
        viewModelScope.launch {
            val result = transport.connect(device)
            if (result.isFailure) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = result.exceptionOrNull()?.localizedMessage
                )
            }
        }
    }

    fun connectManualIp(ip: String, port: String) {
        val address = "$ip:$port"
        val manualDevice = RemoteDevice(
            id = "manual-$address",
            name = "Manual Peer ($address)",
            transportType = TransportType.LOCAL_NETWORK,
            address = address
        )
        connect(manualDevice)
    }

    fun disconnect() {
        viewModelScope.launch {
            transport.disconnect()
        }
    }
}
