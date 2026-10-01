package com.itantra.app.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.presentation.theme.*

@Composable
fun WelcomeScreen(
    onContinueClicked: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // App Brand Icon
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .background(TacticalCyan.copy(alpha = 0.15f))
                        .border(2.dp, TacticalCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Podcasts,
                        contentDescription = "iTantra Transceiver",
                        tint = TacticalCyan,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "iTantra",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )

                Text(
                    text = "OFFLINE NEURAL TRANSCEIVER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = TacticalCyan,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Voice-like communication using offline speech processing and compact text transmission.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            // Key Capability & Limitation Notice Card
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Offline AI Badge
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(TacticalGreen)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "100% Offline AI Operation",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Zero cellular data, zero cloud APIs, and zero SIM card required for speech processing.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Mandatory Communication Limitation Notice
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = TacticalAmber.copy(alpha = 0.12f)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(TacticalAmber.copy(alpha = 0.5f))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Limitation Notice",
                            tint = TacticalAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "An available physical communication link (Bluetooth, Wi-Fi Direct, or tactical radio) is required to transfer messages between devices.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Bottom Action
            Button(
                onClick = onContinueClicked,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Text(
                    text = "INITIALIZE TERMINAL →",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.Black
                )
            }
        }
    }
}
