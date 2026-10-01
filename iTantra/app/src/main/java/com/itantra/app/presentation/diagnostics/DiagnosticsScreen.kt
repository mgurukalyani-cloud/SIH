package com.itantra.app.presentation.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.itantra.app.core.logging.LogEntry
import com.itantra.app.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit
) {
    val report by viewModel.report.collectAsState()
    val logs by viewModel.systemLogs.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Telemetry & Benchmarks", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.runBenchmark() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-run Benchmark")
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs")
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
            // Bandwidth Reduction Metrics
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "BANDWIDTH & REDUCTION BENCHMARKS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(label = "Raw Audio (3s)", value = "96.0 KB")
                        MetricBlock(label = "Text Frame", value = "${report.textPayloadSizeBytes} B")
                        MetricBlock(label = "Data Reduction", value = "${report.dataReductionPercent}%", highlightColor = TacticalGreen)
                    }
                }
            }

            // Latency & Memory Footprint Metrics
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "EDGE AI INFERENCE PERFORMANCE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = TacticalCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MetricBlock(label = "STT Latency", value = "${report.sttLatencyMs} ms")
                        MetricBlock(label = "Real-Time Factor", value = "${report.sttRtf}x")
                        MetricBlock(label = "RAM Footprint", value = "${report.ramUsageMb} MB")
                    }
                }
            }

            // Live Tactical Audit Log Stream
            Text(
                text = "REAL-TIME DIAGNOSTIC AUDIT LOGS (${logs.size})",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TacticalCyan
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF040711)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No log events recorded yet.", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(logs.reversed()) { entry ->
                            Text(
                                text = entry.formatted(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = when (entry.level) {
                                    LogEntry.Level.ERROR -> TacticalRed
                                    LogEntry.Level.WARN -> TacticalAmber
                                    LogEntry.Level.INFO -> TacticalCyan
                                    LogEntry.Level.DEBUG -> Color.Gray
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricBlock(
    label: String,
    value: String,
    highlightColor: Color = Color.Unspecified
) {
    Column {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = if (highlightColor != Color.Unspecified) highlightColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
