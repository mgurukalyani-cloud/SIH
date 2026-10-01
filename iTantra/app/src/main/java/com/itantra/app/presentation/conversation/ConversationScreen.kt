package com.itantra.app.presentation.conversation

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.core.common.toFormattedTime
import com.itantra.app.domain.model.DeliveryStatus
import com.itantra.app.domain.model.Message
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    viewModel: ConversationViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPtt: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tactical Mesh Channel", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("${messages.size} message(s) logged", fontFamily = FontFamily.Monospace, fontSize = 10.sp, color = TacticalCyan)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.stopPlayback() }) {
                        Icon(Icons.Default.VolumeOff, contentDescription = "Stop Audio", tint = TacticalRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            // Text Input Fallback Bar (Section 5 Layer 1: "Provide text input as a fallback")
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToPtt) {
                        Icon(Icons.Default.Mic, contentDescription = "Push to Talk", tint = TacticalCyan)
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("టైప్ చేయండి (Text fallback)...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            if (textInput.isNotBlank()) {
                                viewModel.sendTextMessage(textInput)
                                textInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = TacticalCyan)
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No messages on this link yet.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onNavigateToPtt,
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan)
                    ) {
                        Text("Speak via PTT", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages, key = { it.messageId }) { msg ->
                    MessageBubble(
                        message = msg,
                        onReplay = { viewModel.replayMessage(msg) },
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(msg.text))
                            Toast.makeText(context, "Copied text to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { viewModel.deleteMessage(msg.messageId) }
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: Message,
    onReplay: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val isSentByMe = !message.isIncoming

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isSentByMe) Alignment.End else Alignment.Start
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isSentByMe) 14.dp else 2.dp,
                bottomEnd = if (isSentByMe) 2.dp else 14.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isEmergency -> TacticalRed.copy(alpha = 0.2f)
                    isSentByMe -> TacticalCyan.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
            border = if (message.isEmergency) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TacticalRed)) else null,
            modifier = Modifier.widthIn(max = 310.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Sender and Emergency Tag
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSentByMe) "You (${message.senderId})" else message.senderId,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (message.isEmergency) TacticalRed else TacticalCyan
                    )

                    if (message.isEmergency) {
                        Text(
                            text = "🚨 SOS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = TacticalRed
                        )
                    } else {
                        Text(
                            text = "${message.originalSizeBytes} B",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Spoken / Transcribed Text
                Text(
                    text = message.text,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Metadata & Actions (Replay, Time, Status)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onReplay,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = "Replay Speech",
                                tint = TacticalGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Text",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = message.timestamp.toFormattedTime(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.deliveryStatus) {
                                DeliveryStatus.DELIVERED -> Icons.Default.DoneAll
                                DeliveryStatus.SENT -> Icons.Default.Check
                                DeliveryStatus.SENDING -> Icons.Default.Sync
                                DeliveryStatus.PENDING, DeliveryStatus.QUEUED -> Icons.Default.Schedule
                                DeliveryStatus.FAILED, DeliveryStatus.EXPIRED -> Icons.Default.ErrorOutline
                                else -> Icons.Default.Circle
                            },
                            contentDescription = "Status",
                            tint = when (message.deliveryStatus) {
                                DeliveryStatus.DELIVERED -> TacticalGreen
                                DeliveryStatus.SENT -> TacticalCyan
                                DeliveryStatus.PENDING -> TacticalAmber
                                DeliveryStatus.FAILED -> TacticalRed
                                else -> Color.Gray
                            },
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
