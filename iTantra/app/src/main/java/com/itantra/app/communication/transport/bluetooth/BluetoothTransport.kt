package com.itantra.app.communication.transport.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
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

class BluetoothTransport(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + Job())
) : CommunicationTransport {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        manager?.adapter
    }

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private val _incomingPackets = MutableSharedFlow<iMFPPacket>(extraBufferCapacity = 64)

    private var serverSocket: BluetoothServerSocket? = null
    private var connectedSocket: BluetoothSocket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private var serverJob: Job? = null
    private var readJob: Job? = null

    companion object {
        private const val TAG = "BluetoothTransport"
        private const val SERVICE_NAME = "iTantra_RFCOMM"
    }

    override fun getTransportType(): TransportType = TransportType.BLUETOOTH

    override fun connectionState(): Flow<ConnectionState> = _connectionState.asStateFlow()

    override fun observeIncomingPackets(): Flow<iMFPPacket> = _incomingPackets.asSharedFlow()

    @SuppressLint("MissingPermission")
    override suspend fun startDiscovery(): Flow<List<RemoteDevice>> = flow {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            emit(emptyList())
            return@flow
        }

        // Return bonded (paired) devices first
        val bonded = adapter.bondedDevices?.map { device ->
            RemoteDevice(
                id = device.address,
                name = device.name ?: "Unknown Device",
                transportType = TransportType.BLUETOOTH,
                address = device.address,
                isPaired = true
            )
        } ?: emptyList()

        emit(bonded)
    }

    override suspend fun stopDiscovery() {}

    @SuppressLint("MissingPermission")
    fun startListening() {
        serverJob?.cancel()
        serverJob = scope.launch {
            try {
                val adapter = bluetoothAdapter ?: return@launch
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME,
                    AppConstants.BLUETOOTH_SERVICE_UUID
                )
                TacticalLogger.i(TAG, "Bluetooth RFCOMM Server listening for connections...")

                while (isActive) {
                    val socket = serverSocket?.accept() ?: break
                    TacticalLogger.i(TAG, "Connected to incoming peer: ${socket.remoteDevice?.name}")
                    manageConnectedSocket(socket, socket.remoteDevice?.name ?: "Peer Bluetooth Node")
                }
            } catch (e: Exception) {
                if (isActive) {
                    TacticalLogger.e(TAG, "Bluetooth Server accept error", e)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(device: RemoteDevice): Result<Unit> = withContext(Dispatchers.IO) {
        val adapter = bluetoothAdapter ?: return@withContext Result.failure(IllegalStateException("Bluetooth not supported"))
        _connectionState.value = ConnectionState.Connecting(device.name)

        try {
            val remoteDevice = adapter.getRemoteDevice(device.address)
            val socket = remoteDevice.createRfcommSocketToServiceRecord(AppConstants.BLUETOOTH_SERVICE_UUID)
            adapter.cancelDiscovery()
            socket.connect()

            manageConnectedSocket(socket, device.name)
            Result.success(Unit)
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Bluetooth connection failed to ${device.address}", e)
            _connectionState.value = ConnectionState.Error("Bluetooth connection failed: ${e.localizedMessage}")
            Result.failure(e)
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket, peerName: String) {
        connectedSocket?.close()
        connectedSocket = socket
        outputStream = DataOutputStream(socket.outputStream)
        inputStream = DataInputStream(socket.inputStream)

        val dev = RemoteDevice(
            id = socket.remoteDevice.address,
            name = peerName,
            transportType = TransportType.BLUETOOTH,
            address = socket.remoteDevice.address,
            isConnected = true
        )
        _connectionState.value = ConnectionState.Connected(dev)

        readJob?.cancel()
        readJob = scope.launch {
            try {
                val input = inputStream ?: return@launch
                while (isActive && socket.isConnected) {
                    val frameLength = input.readInt()
                    if (frameLength in 14..65535) {
                        val buffer = ByteArray(frameLength)
                        input.readFully(buffer)
                        val packet = iMFPPacket.parse(buffer)
                        if (packet != null) {
                            TacticalLogger.i(TAG, "Bluetooth RX: ${packet.totalFrameSize} bytes")
                            _incomingPackets.emit(packet)
                        } else {
                            TacticalLogger.w(TAG, "Corrupted Bluetooth packet dropped")
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    TacticalLogger.w(TAG, "Bluetooth connection lost: ${e.localizedMessage}")
                    disconnect()
                }
            }
        }
    }

    override suspend fun send(packet: iMFPPacket): Result<Unit> = withContext(Dispatchers.IO) {
        val out = outputStream
        val socket = connectedSocket
        if (out == null || socket == null || !socket.isConnected) {
            return@withContext Result.failure(IllegalStateException("Bluetooth link disconnected. Message placed in offline queue."))
        }

        try {
            val bytes = packet.toByteArray()
            out.writeInt(bytes.size)
            out.write(bytes)
            out.flush()
            TacticalLogger.i(TAG, "Bluetooth TX: ${bytes.size} bytes successfully dispatched")
            Result.success(Unit)
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Bluetooth send error", e)
            disconnect()
            Result.failure(e)
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        try {
            readJob?.cancel()
            outputStream?.close()
            inputStream?.close()
            connectedSocket?.close()
            connectedSocket = null
            outputStream = null
            inputStream = null
            _connectionState.value = ConnectionState.Disconnected
            TacticalLogger.i(TAG, "Bluetooth link closed")
        } catch (e: Exception) {
            TacticalLogger.e(TAG, "Error closing Bluetooth link", e)
        }
    }

    override fun isConnected(): Boolean {
        return connectedSocket?.isConnected == true
    }

    fun release() {
        serverJob?.cancel()
        readJob?.cancel()
        try {
            serverSocket?.close()
            connectedSocket?.close()
        } catch (ignored: Exception) {}
    }
}
