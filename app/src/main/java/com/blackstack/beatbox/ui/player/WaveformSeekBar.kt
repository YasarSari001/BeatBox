package com.blackstack.beatbox.ui.player

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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.components.formatTime
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

/**
 * Dalga formu biçiminde ilerleme çubuğu.
 *
 * Çubuk yükseklikleri şarkı kimliğinden türetilen sabit bir zarf ile çizilir;
 * böylece aynı şarkıda her açılışta aynı dalga görünür. Oynatma sırasında
 * gerçek visualizer verisi çalma konumunun çevresindeki çubukları canlandırır.
 * Dokunma veya sürükleme ile konum değiştirilir; sürükleme sırasında playhead
 * üzerinde o konumun zamanı balon içinde gösterilir.
 */
@Composable
fun WaveformSeekBar(
    progress: Float,
    /** Toplam süre; sürükleme balonundaki zaman hesabı için. */
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
    val shown = (scrubFraction ?: progress).coerceIn(0f, 1f)
    val envelope = remember(audioKey) { waveformEnvelope(audioKey) }

    val energy = remember(liveWaveform, isPlaying) {
        if (!isPlaying || liveWaveform.isEmpty()) {
            0f
        } else {
            liveWaveform.average().toFloat().coerceIn(0f, 1f)
        }
    }

    val measurer = rememberTextMeasurer()
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
            .pointerInput(audioKey) {
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
            val height = (maxBarHeight * envelope[index] * liveBoost)
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

        // Sürükleme sırasında zaman balonu: parmağın hedeflediği konumu
        // gösterir, kenarlardan taşmaz.
        if (scrubFraction != null) {
            val layout = measurer.measure(
                AnnotatedString(formatTime((shown * durationMs).toLong())),
                style = TextStyle(
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            val padH = 8.dp.toPx()
            val padV = 4.dp.toPx()
            val bubbleWidth = layout.size.width + padH * 2f
            val bubbleHeight = layout.size.height + padV * 2f
            val bubbleX = (playheadX - bubbleWidth / 2f)
                .coerceIn(0f, (size.width - bubbleWidth).coerceAtLeast(0f))
            val bubbleY = (centerY - bubbleHeight / 2f)
                .coerceIn(0f, (size.height - bubbleHeight).coerceAtLeast(0f))
            drawRoundRect(
                color = Color(0xFF14161F).copy(alpha = 0.96f),
                topLeft = Offset(bubbleX, bubbleY),
                size = Size(bubbleWidth, bubbleHeight),
                cornerRadius = CornerRadius(bubbleHeight / 2f)
            )
            drawText(layout, topLeft = Offset(bubbleX + padH, bubbleY + padV))
        }
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
