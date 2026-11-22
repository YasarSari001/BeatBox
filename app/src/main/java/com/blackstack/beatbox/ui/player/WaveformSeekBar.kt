package com.blackstack.beatbox.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlin.math.abs
import kotlin.math.sin

/**
 * Dalga formu biçiminde ilerleme çubuğu.
 *
 * Çubuk yükseklikleri şarkı kimliğinden türetilen sabit bir zarf ile çizilir;
 * böylece aynı şarkıda her açılışta aynı dalga görünür. Oynatma sırasında
 * gerçek visualizer verisi çalma konumunun çevresindeki çubukları canlandırır.
 * Dokunma veya sürükleme ile konum değiştirilir; sürükleme sırasında üstteki
 * zaman etiketleri parmakla birlikte güncellenir.
 */
@Composable
fun WaveformSeekBar(
    progress: Float,
    durationMs: Long,
    audioKey: Long,
    isPlaying: Boolean,
    liveWaveform: FloatArray,
    contentDescription: String,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    val reducedMotion = LocalReducedMotion.current
    val shown = (scrubFraction ?: progress).coerceIn(0f, 1f)
    val envelope = remember(audioKey) { waveformEnvelope(audioKey) }

    val energy = remember(liveWaveform, isPlaying) {
        if (!isPlaying || liveWaveform.isEmpty()) {
            0f
        } else {
            liveWaveform.average().toFloat().coerceIn(0f, 1f)
        }
    }

    val pulse = if (!isPlaying || reducedMotion) {
        1f
    } else {
        val transition = rememberInfiniteTransition(label = "waveform_pulse")
        val value by transition.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1100, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "waveform_pulse_value"
        )
        value
    }

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val playedBrush = remember(primary, secondary) {
        Brush.horizontalGradient(listOf(primary, secondary))
    }
    val idleColor = Color.White.copy(alpha = 0.16f)

    val seekTo: (Float) -> Unit = { raw ->
        val fraction = raw.coerceIn(0f, 1f)
        scrubFraction = fraction
        onScrub(fraction)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                this.contentDescription = contentDescription
                progressBarRangeInfo = ProgressBarRangeInfo(shown, 0f..1f)
            }
            .pointerInput(audioKey, durationMs) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    var fraction = (down.position.x / size.width).coerceIn(0f, 1f)
                    scrubFraction = fraction
                    onScrub(fraction)
                    drag(down.id) { change ->
                        change.consume()
                        fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        scrubFraction = fraction
                        onScrub(fraction)
                    }
                    scrubFraction = null
                    onScrubFinished(fraction)
                }
            }
    ) {
        val count = envelope.size
        val slot = size.width / count
        val barWidth = (slot * 0.55f).coerceAtLeast(1.5f)
        val radius = CornerRadius(barWidth / 2f)
        val centerY = size.height / 2f
        val maxBarHeight = size.height * 0.46f

        for (index in 0 until count) {
            val barFraction = (index + 0.5f) / count
            val distance = abs(barFraction - shown)
            val liveBoost = 1f + energy * (1f - (distance * 6f)).coerceIn(0f, 1f) * 0.6f
            val height = (maxBarHeight * envelope[index] * liveBoost * pulse)
                .coerceIn(2f, maxBarHeight)
            val left = index * slot + (slot - barWidth) / 2f
            val top = centerY - height
            if (barFraction <= shown) {
                drawRoundRect(
                    brush = playedBrush,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height * 2f),
                    cornerRadius = radius
                )
            } else {
                drawRoundRect(
                    color = idleColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, height * 2f),
                    cornerRadius = radius
                )
            }
        }

        val playheadX = (shown * size.width).coerceIn(1f, size.width - 1f)
        drawLine(
            color = Color.White.copy(alpha = 0.82f),
            start = Offset(playheadX, 0f),
            end = Offset(playheadX, size.height),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawCircle(
            color = primary.copy(alpha = 0.30f),
            radius = 8.dp.toPx(),
            center = Offset(playheadX, centerY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.5.dp.toPx(),
            center = Offset(playheadX, centerY)
        )
    }
}

/**
 * Şarkı kimliğinden türetilen, komşu ortalamasıyla yumuşatılmış dalga zarfı.
 * Aynı şarkı için her zaman aynı sonucu verir; rastgelelik uygulama oturumuna
 * bağlı değildir.
 */
private fun waveformEnvelope(key: Long): FloatArray {
    val random = java.util.Random(key * 31L + 17L)
    val raw = FloatArray(64) { 0.35f + random.nextFloat() * 0.65f }
    return FloatArray(raw.size) { index ->
        val previous = raw[(index + raw.size - 1) % raw.size]
        val next = raw[(index + 1) % raw.size]
        val smooth = (previous + raw[index] * 2f + next) / 4f
        (smooth * (0.78f + 0.22f * sin(index * 0.55f))).coerceIn(0.16f, 1f)
    }
}
