package com.itantra.app.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.ConnectionState
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToPtt: () -> Unit,
    onNavigateToConversation: () -> Unit,
    onNavigateToConnection: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "iTantra // ${uiState.callsign}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "OFFLINE SPEECH TRANSCEIVER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TacticalCyan
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToDiagnostics) {
                        Icon(Icons.Default.Analytics, contentDescription = "Diagnostics", tint = TacticalCyan)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hardware Status Bar (Battery, Zero Internet Badge, ID)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🔋 88% | NO SIM | OFFLINE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "ID: ${uiState.deviceId}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyan
                )
            }

            // AI Pipeline Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isAiReady) TacticalGreen else TacticalRed)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SPEECH PIPELINE STATUS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TacticalCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.aiStatusLabel,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Model: ${uiState.currentLanguage.displayName} (${uiState.currentLanguage.nativeName})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Transport Link Status Card (Clickable to jump to Connection screen)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when (uiState.connectionState) {
                        is ConnectionState.Connected -> TacticalGreen.copy(alpha = 0.12f)
                        is ConnectionState.Connecting -> TacticalAmber.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToConnection() }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (uiState.connectionState) {
                            is ConnectionState.Connected -> Icons.Default.Link
                            is ConnectionState.Connecting -> Icons.Default.Sync
                            else -> Icons.Default.LinkOff
                        },
                        contentDescription = "Link Status",
                        tint = when (uiState.connectionState) {
                            is ConnectionState.Connected -> TacticalGreen
                            is ConnectionState.Connecting -> TacticalAmber
                            else -> TacticalRed
                        },
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TACTICAL LINK INTERFACE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = TacticalCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (val state = uiState.connectionState) {
                                is ConnectionState.Connected -> "Connected: ${state.device.name}"
                                is ConnectionState.Connecting -> "Connecting to ${state.deviceName}..."
                                is ConnectionState.Error -> "Link Error: ${state.message}"
                                is ConnectionState.Disconnected -> "No Communication Link (Standby)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open Connections",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Store & Forward Queue Indicator
            if (uiState.pendingCount > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = TacticalAmber.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = "Pending", tint = TacticalAmber)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${uiState.pendingCount} message(s) pending in offline queue. Auto-transmitting when link connects.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Large PTT Action Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNavigateToPtt,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Push to Talk",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "PUSH TO TALK (PTT)",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color.Black
                            )
                            Text(
                                text = "Speak in ${uiState.currentLanguage.displayName} → Send as Compact Text",
                                fontSize = 12.sp,
                                color = Color.Black.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Emergency SOS Button
                Button(
                    onClick = {
                        viewModel.triggerEmergencySOS { sos ->
                            onNavigateToConversation()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = "🚨 BROADCAST EMERGENCY SOS (PRIO 3)",
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Navigation Shortcuts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToConversation,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Messages", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onNavigateToConnection,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Links", fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
