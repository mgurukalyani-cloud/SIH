package com.itantra.app.presentation.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.itantra.app.presentation.theme.TacticalCyan
import com.itantra.app.presentation.theme.TacticalGreen
import kotlin.math.sin

@Composable
fun WaveformVisualizer(
    rms: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f
        val numBars = 40
        val barSpacing = width / numBars

        val normalizedRms = if (isRecording) (rms / 800f).coerceIn(0.08f, 1.0f) else 0.04f

        for (i in 0 until numBars) {
            val x = i * barSpacing + barSpacing / 2f
            // Harmonic wave modulated by RMS energy
            val sineFactor = (sin(i * 0.45 + System.currentTimeMillis() * 0.006) * 0.5 + 0.5).toFloat()
            val barHeight = (height * normalizedRms * (0.3f + 0.7f * sineFactor)).coerceAtLeast(3f)

            val color = if (normalizedRms > 0.4f) TacticalGreen else TacticalCyan

            drawLine(
                color = if (isRecording) color else Color.Gray.copy(alpha = 0.3f),
                start = Offset(x, centerY - barHeight / 2f),
                end = Offset(x, centerY + barHeight / 2f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}
