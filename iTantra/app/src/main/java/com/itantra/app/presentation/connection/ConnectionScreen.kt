package com.itantra.app.presentation.connection

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.domain.model.RemoteDevice
import com.itantra.app.domain.model.TransportType
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    viewModel: ConnectionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var manualIp by remember { mutableStateOf(uiState.ipInput) }
    var manualPort by remember { mutableStateOf(uiState.portInput) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Radio & Link Transports", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.startScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Scan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Transport Type Selector Tabs
            TabRow(
                selectedTabIndex = uiState.activeTransport.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = TacticalCyan
            ) {
                Tab(
                    selected = uiState.activeTransport == TransportType.MOCK,
                    onClick = { viewModel.setTransport(TransportType.MOCK) },
                    text = { Text("Mock", fontSize = 12.sp) }
                )
                Tab(
                    selected = uiState.activeTransport == TransportType.BLUETOOTH,
                    onClick = { viewModel.setTransport(TransportType.BLUETOOTH) },
                    text = { Text("Bluetooth", fontSize = 12.sp) }
                )
                Tab(
                    selected = uiState.activeTransport == TransportType.LOCAL_NETWORK,
                    onClick = { viewModel.setTransport(TransportType.LOCAL_NETWORK) },
                    text = { Text("Wi-Fi TCP", fontSize = 12.sp) }
                )
            }

            // Active Connection Banner
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (uiState.connectionState) {
                        is ConnectionState.Connected -> TacticalGreen.copy(alpha = 0.15f)
                        is ConnectionState.Connecting -> TacticalAmber.copy(alpha = 0.15f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STATUS: ${when (val s = uiState.connectionState) {
                                is ConnectionState.Connected -> "CONNECTED TO ${s.device.name.uppercase()}"
                                is ConnectionState.Connecting -> "CONNECTING..."
                                is ConnectionState.Error -> "DISCONNECTED (ERROR)"
                                is ConnectionState.Disconnected -> "STANDBY (NO CARRIER)"
                            }}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (uiState.connectionState) {
                                is ConnectionState.Connected -> TacticalGreen
                                is ConnectionState.Connecting -> TacticalAmber
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            }
                        )
                        if (uiState.connectionState is ConnectionState.Connected) {
                            Text(
                                text = "Transport: ${uiState.activeTransport.name} | Zero Internet Link Active",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }

                    if (uiState.connectionState is ConnectionState.Connected) {
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalRed)
                        ) {
                            Text("Disconnect", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Wi-Fi Direct Manual IP/Port Input
            if (uiState.activeTransport == TransportType.LOCAL_NETWORK) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "DIRECT PEER TCP SOCKET (LOCAL WI-FI / HOTSPOT)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TacticalCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = manualIp,
                                onValueChange = { manualIp = it },
                                label = { Text("Peer IP") },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = manualPort,
                                onValueChange = { manualPort = it },
                                label = { Text("Port") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { viewModel.connectManualIp(manualIp, manualPort) },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connect Peer Socket", color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Text(
                text = "AVAILABLE / NEARBY PEER NODES",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalCyan
            )

            // Device List
            if (uiState.discoveredDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isScanning) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text("No nodes discovered on this transport. Tap scan to refresh.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(uiState.discoveredDevices, key = { it.id }) { device ->
                        DeviceItemRow(
                            device = device,
                            onConnectClicked = { viewModel.connect(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItemRow(
    device: RemoteDevice,
    onConnectClicked: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    text = "${device.transportType.name} // ${device.address}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Button(
                onClick = onConnectClicked,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Connect", fontSize = 12.sp, color = androidx.compose.ui.graphics.Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
