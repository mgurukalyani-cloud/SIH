package com.itantra.app.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.domain.model.AppLanguage
import com.itantra.app.modelmanagement.registry.ModelRegistry
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    if (uiState.clearSuccess) {
        Toast.makeText(context, "Local message history cleared", Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Node Identity
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TERMINAL CALLSIGN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = uiState.nodeCallsign,
                        onValueChange = { viewModel.updateCallsign(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }

            // Language & Installed Model Catalog
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INDIAN SPEECH MODELS (10 TARGET LANGUAGES)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AppLanguage.entries.forEach { lang ->
                        val isAvailable = ModelRegistry.isLanguageAvailable(lang)
                        val meta = ModelRegistry.getMetadata(lang)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = isAvailable) { viewModel.updateLanguage(lang) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${lang.displayName} (${lang.nativeName})",
                                    fontWeight = if (uiState.selectedLanguage == lang) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isAvailable) "Ready // ${meta.version} (${meta.license})" else "Model not installed or not validated for this device.",
                                    fontSize = 11.sp,
                                    color = if (isAvailable) TacticalGreen else TacticalRed
                                )
                            }
                            RadioButton(
                                selected = uiState.selectedLanguage == lang,
                                onClick = { if (isAvailable) viewModel.updateLanguage(lang) },
                                enabled = isAvailable
                            )
                        }
                    }
                }
            }

            // VAD Audio Gate Sensitivity Slider
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VAD SPEECH SENSITIVITY THRESHOLD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Current RMS Threshold: ${uiState.vadThreshold.toInt()}",
                        fontSize = 13.sp
                    )
                    Slider(
                        value = uiState.vadThreshold,
                        onValueChange = { viewModel.updateVadThreshold(it) },
                        valueRange = 200f..1000f
                    )
                    Text(
                        text = "Lower values pick up faint whispers; higher values suppress heavy ambient wind or engine noise.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Compression & Playback Toggles
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Speech Synthesis", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Auto-play incoming messages via TTS speaker upon reception.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = uiState.isAutoPlayTts,
                            onCheckedChange = { viewModel.toggleAutoPlay(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Adaptive Payload Compression", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Compress messages exceeding 35 bytes using Deflate dictionary.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = uiState.isCompressionEnabled,
                            onCheckedChange = { viewModel.toggleCompression(it) }
                        )
                    }
                }
            }

            // Privacy & Database Management
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "LOCAL DATA & PRIVACY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All voice audio is processed in volatile RAM. No raw audio recordings are stored to flash.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.clearMessageHistory() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Purge Local Message Database")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
