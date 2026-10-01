package com.itantra.app.communication.transport.mock

import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class MockLoopbackTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : CommunicationTransport {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _incomingPackets = MutableSharedFlow<iMFPPacket>(extraBufferCapacity = 64)
    private var simulatedLatencyMs: Long = 180L

    val mockDevices = listOf(
        RemoteDevice(
            id = "mock-bravo-01",
            name = "Tactical Node Bravo (Field Hospital)",
            transportType = TransportType.MOCK,
            address = "MOCK:00:1A:7D:F2:01",
            rssi = -64,
            isPaired = true
        ),
        RemoteDevice(
            id = "mock-charlie-02",
            name = "Tactical Node Charlie (SAR Basecamp)",
            transportType = TransportType.MOCK,
            address = "MOCK:00:1A:7D:F2:02",
            rssi = -78,
            isPaired = false
        )
    )

    override fun getTransportType(): TransportType = TransportType.MOCK

    override fun connectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIncomingPackets(): Flow<iMFPPacket> = _incomingPackets.asSharedFlow()

    override suspend fun startDiscovery(): Flow<List<RemoteDevice>> = flow {
        emit(emptyList())
        delay(300)
        emit(mockDevices.take(1))
        delay(400)
        emit(mockDevices)
    }

    override suspend fun stopDiscovery() {}

    override suspend fun connect(device: RemoteDevice): Result<Unit> {
        _connectionState.value = ConnectionState.Connecting(device.name)
        delay(400) // Sim connection handshake
        _connectionState.value = ConnectionState.Connected(device.copy(isConnected = true))
        return Result.success(Unit)
    }

    override suspend fun disconnect() {
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(packet: iMFPPacket): Result<Unit> {
        if (!isConnected()) {
            return Result.failure(IllegalStateException("Transport disconnected. Storing message in local queue."))
        }

        // Simulate over-the-air transmission delay
        scope.launch {
            delay(simulatedLatencyMs)
            // Loopback packet to receiver observer with ACK response
            _incomingPackets.emit(packet)
        }

        return Result.success(Unit)
    }

    override fun isConnected(): Boolean {
        return _connectionState.value is ConnectionState.Connected
    }
}
