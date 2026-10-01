package com.itantra.app.presentation.recording

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.presentation.theme.TacticalAmber

@Composable
fun LowConfidenceReviewDialog(
    originalText: String,
    confidence: Float,
    onSend: (String) -> Unit,
    onRecordAgain: () -> Unit,
    onCancel: () -> Unit
) {
    var textValue by remember { mutableStateOf(originalText) }
    var isEditing by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        icon = {
            Icon(Icons.Default.Warning, contentDescription = "Low Confidence", tint = TacticalAmber)
        },
        title = {
            Text(
                text = "Review Recognized Speech",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Speech recognition confidence is low (${(confidence * 100).toInt()}%). Please review before sending.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )

                if (isEditing) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Edit Transcribed Message") }
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = textValue,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(textValue) },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber)
            ) {
                Text("Send Message", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { isEditing = !isEditing }) {
                    Text(if (isEditing) "Done Edit" else "Edit")
                }
                TextButton(onClick = onRecordAgain) {
                    Text("Record Again")
                }
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    )
}
