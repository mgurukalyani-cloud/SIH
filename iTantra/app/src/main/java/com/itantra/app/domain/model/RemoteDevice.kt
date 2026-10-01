package com.itantra.app.domain.model

enum class TransportType {
    BLUETOOTH,
    WIFI_DIRECT,
    LOCAL_NETWORK,
    MOCK
}

data class RemoteDevice(
    val id: String,
    val name: String,
    val transportType: TransportType,
    val address: String,
    val rssi: Int = 0,
    val isPaired: Boolean = false,
    val isConnected: Boolean = false
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    data class Connecting(val deviceName: String) : ConnectionState()
    data class Connected(val device: RemoteDevice) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
