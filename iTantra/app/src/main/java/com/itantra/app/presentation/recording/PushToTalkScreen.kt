package com.itantra.app.presentation.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.presentation.theme.*
import com.itantra.app.speech.vad.VadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushToTalkScreen(
    viewModel: RecordingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToConversation: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Push-to-Talk (PTT)", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Language & VAD Status Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isRecording) TacticalGreen else Color.Gray)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "VAD: ${when (uiState.vadState) {
                            VadState.SILENCE -> "SILENCE DETECTED"
                            VadState.SPEECH_STARTED -> "SPEECH STARTED"
                            VadState.IN_SPEECH -> "VOICE ACTIVE"
                            VadState.SPEECH_ENDED -> "UTTERANCE COMPLETE"
                        }}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.isRecording) TacticalGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Waveform Canvas
                WaveformVisualizer(
                    rms = uiState.currentRms,
                    isRecording = uiState.isRecording
                )

                // Error Banner
                if (uiState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = TacticalAmber.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            color = TacticalAmber,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }

            // Central PTT Button Area
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                // Large Tactical Round Button
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            if (uiState.isRecording) TacticalGreen.copy(alpha = 0.25f)
                            else TacticalCyan.copy(alpha = 0.15f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    viewModel.startPtt()
                                    tryAwaitRelease()
                                    viewModel.stopPtt()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(124.dp)
                            .clip(CircleShape)
                            .background(if (uiState.isRecording) TacticalGreen else TacticalCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "PTT Button",
                            tint = Color.Black,
                            modifier = Modifier.size(54.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (uiState.isRecording) "RECORDING... RELEASE TO SEND" else "PRESS AND HOLD TO SPEAK",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = if (uiState.isRecording) TacticalGreen else TacticalCyan
                )

                Text(
                    text = "Language: ${uiState.language.displayName} (${uiState.language.nativeName})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )

                if (uiState.isProcessingStt) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decoding Telugu phonemes locally...", fontSize = 12.sp)
                    }
                }
            }

            // Message Confirmation Card (Section 12 Screen 5)
            if (uiState.showConfirmationCard && uiState.recognizedText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECOGNIZED SPEECH",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TacticalCyan,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Conf: ${(uiState.confidenceScore * 100).toInt()}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (uiState.confidenceScore > 0.75f) TacticalGreen else TacticalAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.recognizedText!!,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Payload: ${uiState.payloadSizeBytes} Bytes",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                text = "Air Time: ~${uiState.estimatedTransmissionTimeMs} ms @ 1.2 kbps",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = TacticalAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.dismissConfirmation() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel")
                            }

                            Button(
                                onClick = {
                                    viewModel.confirmAndSend()
                                    onNavigateToConversation()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                                modifier = Modifier.weight(1.5f)
                            ) {
                                Text("Transmit →", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Low Confidence Review Modal (Section 7)
    if (uiState.showLowConfidenceDialog && uiState.recognizedText != null) {
        LowConfidenceReviewDialog(
            originalText = uiState.recognizedText!!,
            confidence = uiState.confidenceScore,
            onSend = { edited ->
                viewModel.confirmAndSend(edited)
                onNavigateToConversation()
            },
            onRecordAgain = {
                viewModel.dismissConfirmation()
            },
            onCancel = {
                viewModel.dismissConfirmation()
            }
        )
    }
}
