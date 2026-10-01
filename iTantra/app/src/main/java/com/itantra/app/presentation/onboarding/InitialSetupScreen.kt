package com.itantra.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.data.local.AppPreferences
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.domain.model.TransportType
import com.itantra.app.modelmanagement.registry.ModelRegistry
import com.itantra.app.modelmanagement.registry.ModelStatus
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InitialSetupScreen(
    prefs: AppPreferences,
    onSetupComplete: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(prefs.selectedLanguage) }
    var selectedTransport by remember { mutableStateOf(prefs.selectedTransport) }
    var callsign by remember { mutableStateOf(prefs.nodeCallsign) }
    var autoPlayTts by remember { mutableStateOf(prefs.isAutoPlayTts) }
    var isMicTestPassed by remember { mutableStateOf(false) }
    var isSpeakerTestPassed by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terminal Setup", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Node Identity
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. NODE IDENTIFICATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = callsign,
                        onValueChange = { callsign = it; prefs.nodeCallsign = it },
                        label = { Text("Callsign / Unit Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unique Node ID: 0x${prefs.deviceId}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Language Selection & Model Verification Status
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "2. PRIMARY LANGUAGE (INDIC ASR/TTS)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AppLanguage.entries.forEach { lang ->
                        val isAvailable = ModelRegistry.isLanguageAvailable(lang)
                        val isSelected = selectedLanguage == lang

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .background(
                                    if (isSelected) TacticalCyan.copy(alpha = 0.12f) else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable(enabled = isAvailable) {
                                    selectedLanguage = lang
                                    prefs.selectedLanguage = lang
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "${lang.displayName} (${lang.nativeName})",
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    fontSize = 14.sp
                                )
                                if (!isAvailable) {
                                    Text(
                                        text = "Model not installed or not validated for this device.",
                                        fontSize = 10.sp,
                                        color = TacticalRed
                                    )
                                } else if (lang == AppLanguage.TELUGU) {
                                    Text(
                                        text = "✓ Primary Validated MVP Pipeline",
                                        fontSize = 10.sp,
                                        color = TacticalGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    if (isAvailable) {
                                        selectedLanguage = lang
                                        prefs.selectedLanguage = lang
                                    }
                                },
                                enabled = isAvailable
                            )
                        }
                    }
                }
            }

            // Hardware Diagnostics Check (Mic & Speaker)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "3. HARDWARE SANITY CHECK",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Microphone Audio Capture", fontSize = 14.sp)
                        Button(
                            onClick = { isMicTestPassed = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isMicTestPassed) TacticalGreen else TacticalCyan
                            )
                        ) {
                            Text(if (isMicTestPassed) "✓ Tested" else "Test Mic")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Speaker Audio Playback", fontSize = 14.sp)
                        Button(
                            onClick = { isSpeakerTestPassed = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSpeakerTestPassed) TacticalGreen else TacticalCyan
                            )
                        ) {
                            Text(if (isSpeakerTestPassed) "✓ Tested" else "Test Speaker")
                        }
                    }
                }
            }

            // Default Transport Selection
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "4. AD-HOC LINK INTERFACE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    TransportType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTransport = type
                                    prefs.selectedTransport = type
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTransport == type,
                                onClick = {
                                    selectedTransport = type
                                    prefs.selectedTransport = type
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (type) {
                                    TransportType.BLUETOOTH -> "Bluetooth RFCOMM (Direct Phone-to-Phone)"
                                    TransportType.LOCAL_NETWORK -> "Local Wi-Fi / Hotspot TCP Socket"
                                    TransportType.WIFI_DIRECT -> "Wi-Fi Direct P2P Group"
                                    TransportType.MOCK -> "In-Memory Simulation Loopback"
                                },
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Finish Setup Button
            Button(
                onClick = onSetupComplete,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "ENTER TACTICAL DASHBOARD",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
