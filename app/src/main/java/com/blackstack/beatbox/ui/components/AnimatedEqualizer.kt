package com.blackstack.beatbox.ui.components
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Çalan şarkıyı gösteren üç çubuklu küçük ekolayzer animasyonu. */
@Composable
fun AnimatedEqualizer(isPlaying: Boolean, color: Color) {
    val inf = rememberInfiniteTransition(label = "equalizer")
    val bars = (0..2).map { index ->
        inf.animateFloat(
            initialValue = 0.2f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(400 + index * 150, 50, LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "eq_$index"
        )
    }
    Canvas(Modifier.size(20.dp)) {
        val barWidth = size.width / 5
        bars.forEachIndexed { i, value ->
            val targetValue = if (isPlaying) value.value else 0.2f
            val barHeight = size.height * targetValue
            drawRoundRect(
                color = color,
                topLeft = Offset(i * barWidth * 2, size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
