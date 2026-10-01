package com.itantra.app.communication.transport

import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.flow.Flow

interface CommunicationTransport {
    fun getTransportType(): TransportType
    fun connectionState(): Flow<ConnectionState>
    fun observeIncomingPackets(): Flow<iMFPPacket>
    
    suspend fun startDiscovery(): Flow<List<RemoteDevice>>
    suspend fun stopDiscovery()
    suspend fun connect(device: RemoteDevice): Result<Unit>
    suspend fun disconnect()
    suspend fun send(packet: iMFPPacket): Result<Unit>
    fun isConnected(): Boolean
}
