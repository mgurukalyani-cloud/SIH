package com.itantra.app.domain.repository

import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.flow.Flow

interface DeviceRepository {
    val connectionState: Flow<ConnectionState>
    val discoveredDevices: Flow<List<RemoteDevice>>
    
    suspend fun startDiscovery(type: TransportType)
    suspend fun stopDiscovery()
    suspend fun connect(device: RemoteDevice)
    suspend fun disconnect()
    fun getLocalNodeId(): Short
    fun getLocalNodeName(): String
}
