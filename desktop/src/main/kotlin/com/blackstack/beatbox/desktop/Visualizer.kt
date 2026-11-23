package com.blackstack.beatbox.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay

/**
 * Çalan sesin gerçek bant enerjisinden beslenen çubuk görselleştirici.
 *
 * Android'deki EnhancedRealTimeVisualizer'ın masaüstü karşılığıdır; veri
 * [AudioEngine.bands] üzerinden gelir. Duraklatıldığında çubuklar söner.
 */
@Composable
fun Visualizer(
    engine: AudioEngine,
    isPlaying: Boolean,
    accent: Color,
    secondary: Color,
    modifier: Modifier = Modifier,
    barCount: Int = BAND_COUNT,
) {
    val values = remember { FloatArray(barCount) }
    var frame by remember { mutableIntStateOf(0) }

    LaunchedEffect(isPlaying) {
        while (true) {
            val source = engine.bands
            for (index in 0 until barCount) {
                // Duraklatıldığında Android'deki gibi sabit bir eko şekli kalır.
                val target = if (isPlaying) {
                    source.getOrElse(index) { 0f }
                } else {
                    idleShape(index, barCount)
                }
                values[index] = if (target > values[index]) {
                    values[index] + (target - values[index]) * 0.6f
                } else {
                    values[index] * 0.85f
                }
            }
            frame++
            delay(if (isPlaying) 33L else 120L)
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            // `frame` okuması Canvas'ı her karede yeniden çizer.
            @Suppress("UNUSED_VARIABLE")
            val tick = frame
            val count = barCount.coerceAtLeast(1)
            val slot = size.width / count
            val barWidth = (slot * 0.52f).coerceAtLeast(2f)
            val radius = CornerRadius(barWidth / 2f, barWidth / 2f)
            val centerY = size.height / 2f
            val brush = Brush.verticalGradient(
                colors = listOf(secondary, accent, accent.copy(alpha = 0.55f)),
                startY = 0f,
                endY = size.height,
            )

            for (index in 0 until count) {
                val amplitude = values.getOrElse(index) { 0f }.coerceIn(0f, 1f)
                val height = (size.height * (0.10f + amplitude * 0.90f)).coerceAtLeast(size.height * 0.06f)
                val left = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(left, centerY - height / 2f),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                    alpha = if (isPlaying) 0.55f + amplitude * 0.45f else 0.42f,
                )
            }
        }
    }
}

/** Çalma yokken görünen sabit eko şekli (ortada uzun, kenarlara doğru kısalır). */
private fun idleShape(index: Int, count: Int): Float {
    val center = (count - 1) / 2f
    val distance = kotlin.math.abs(index - center) / center.coerceAtLeast(1f)
    return (0.55f - distance * 0.38f).coerceAtLeast(0.12f)
}
