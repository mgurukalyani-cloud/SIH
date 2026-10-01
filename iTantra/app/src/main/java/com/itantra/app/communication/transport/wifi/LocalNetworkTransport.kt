package com.itantra.app.communication.transport.wifi

import com.itantra.app.core.common.AppConstants
import com.itantra.app.core.logging.TacticalLogger
import com.itantra.app.communication.packet.iMFPPacket
import com.itantra.app.communication.transport.CommunicationTransport
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class LocalNetworkTransport(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) : CommunicationTransport {

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _incomingPackets = MutableSharedFlow<iMFPPacket>(extraBufferCapacity = 64)

    private var serverSocket: ServerSocket? = null
    private var activeSocket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var serverJob: Job? = null
    private var clientJob: Job? = null

    companion object {
        private const val TAG = "LocalNetworkTransport"
    }

    override fun getTransportType(): TransportType = TransportType.LOCAL_NETWORK

    override fun connectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIncomingPackets(): Flow<iMFPPacket> = _incomingPackets.asSharedFlow()

    override suspend fun startDiscovery(): Flow<List<RemoteDevice>> = flow {
        // In local network/hotspot, server device acts as broadcast anchor
        val localAnchor = RemoteDevice(
            id = "lan-host-peer",
            name = "Local Ad-Hoc TCP Server",
            transportType = TransportType.LOCAL_NETWORK,
            address = "127.0.0.1:${AppConstants.DEFAULT_LOCAL_TCP_PORT}",
            isPaired = true
        )
        emit(listOf(localAnchor))
    }

    override suspend fun stopDiscovery() {}

    fun startServer(port: Int = AppConstants.DEFAULT_LOCAL_TCP_PORT) {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                serverSocket?.close()
                serverSocket = ServerSocket(port)
                TacticalLogger.i(TAG, "Local TCP Server listening on port $port")

                while (isActive) {
                    val client = serverSocket?.accept() ?: break
                    TacticalLogger.i(TAG, "Accepted incoming peer: ${client.remoteSocketAddress}")
                    handleSocketConnection(client, "Peer ${client.inetAddress.hostAddress}")
                }
            } catch (e: Exception) {
                if (isActive) {
                    TacticalLogger.e(TAG, "Server socket error", e)
                }
            }
        }
    }

    override suspend fun connect(device: RemoteDevice): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _connectionState.value = ConnectionState.Connecting(device.name)
            val parts = device.address.split(":")
            val host = parts[0]
            val port = if (parts.size > 1) parts[1].toInt() else AppConstants.DEFAULT_LOCAL_TCP_PORT

            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 4000)
            handleSocketConnection(socket, device.name)
            Result.success(Unit)
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Connection failed to ${device.address}", e)
            _connectionState.value = ConnectionState.Error("Failed to connect: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    private fun handleSocketConnection(socket: Socket, peerName: String) {
        activeSocket?.close()
        activeSocket = socket
        outputStream = DataOutputStream(socket.getOutputStream())
        inputStream = DataInputStream(socket.getInputStream())

        val remoteDevice = RemoteDevice(
            id = socket.remoteSocketAddress.toString(),
            name = peerName,
            transportType = TransportType.LOCAL_NETWORK,
            address = socket.remoteSocketAddress.toString(),
            isConnected = true
        )
        _connectionState.value = ConnectionState.Connected(remoteDevice)

        // Read incoming packets loop
        clientJob?.cancel()
        clientJob = scope.launch {
            try {
                val input = inputStream ?: return@launch
                while (isActive && socket.isConnected && !socket.isClosed) {
                    val frameLength = input.readInt()
                    if (frameLength in 14..65535) {
                        val buffer = ByteArray(frameLength)
                        input.readFully(buffer)
                        val packet = iMFPPacket.parse(buffer)
                        if (packet != null) {
                            TacticalLogger.i(TAG, "Received valid iMFP frame: ${packet.totalFrameSize} bytes")
                            _incomingPackets.emit(packet)
                        } else {
                            TacticalLogger.w(TAG, "Received corrupted iMFP frame (CRC failed)")
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    TacticalLogger.w(TAG, "Socket read closed: ${e.localizedMessage}")
                    disconnect()
                }
            }
        }
    }

    override suspend fun send(packet: iMFPPacket): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        val socket = activeSocket
        if (out == null || socket == null || !socket.isConnected || socket.isClosed) {
            return@withContext Result.failure(IllegalStateException("No active network connection. Packet queued."))
        }

        try {
            val bytes = packet.toByteArray()
            out.writeInt(bytes.size)
            out.write(bytes)
            out.flush()
            TacticalLogger.i(TAG, "Transmitted ${bytes.size} bytes over TCP link")
            Result.success(Unit)
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Failed to send packet over socket", e)
            disconnect()
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            clientJob?.cancel()
            outputStream?.close()
            inputStream?.close()
            activeSocket?.close()
            activeSocket = null
            outputStream = null
            inputStream = null
            _connectionState.value = ConnectionState.Disconnected
            TacticalLogger.i(TAG, "Disconnected local network link")
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Error closing connection", e)
        }
    }

    override fun isConnected(): Boolean {
        return activeSocket?.isConnected == true && !activeSocket!!.isClosed
    }

    fun release() {
        serverJob?.cancel()
        clientJob?.cancel()
        try {
            serverSocket?.close()
            activeSocket?.close()
        } catch (ignored: Exception) {}
    }
}
