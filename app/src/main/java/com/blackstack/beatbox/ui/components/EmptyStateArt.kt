package com.blackstack.beatbox.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.ui.theme.LocalReducedMotion

/**
 * Boş durum illüstrasyon türleri.
 *
 * Her tür, aynı cam plaka ve aurora ışığı üzerinde farklı bir çizgi
 * kompozisyonu kullanır; böylece ekranlar arasında tutarlı bir dil oluşur.
 */
enum class EmptyArtKind { MUSIC, FOLDER, SEARCH, PLAYLIST, EQUALIZER, SLEEP }

/**
 * Aurora Glass boş durum illüstrasyonu.
 *
 * Tamamı Canvas ile çizilir; varlık dosyası gerektirmez ve her yoğunlukta
 * keskin kalır. Işık nabzı "animasyonları kaldır" ayarına saygı duyar.
 *
 * @param kind Çizilecek kompozisyon.
 * @param size Kare çizim alanı.
 */
@Composable
fun EmptyStateArt(
    kind: EmptyArtKind,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    accent: Color = MaterialTheme.colorScheme.primary,
    secondaryAccent: Color = MaterialTheme.colorScheme.secondary
) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "empty_art")
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = if (reducedMotion) 0.18f else 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3_600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_art_glow"
    )

    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        val center = Offset(s * 0.5f, s * 0.48f)

        // Aurora ışığı ve cam plaka: tüm türlerde ortak zemin.
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = glowAlpha), Color.Transparent),
                center = center,
                radius = s * 0.52f
            ),
            radius = s * 0.52f,
            center = center
        )
        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(Color.White.copy(alpha = 0.055f), Color.White.copy(alpha = 0.015f))
            ),
            topLeft = Offset(s * 0.09f, s * 0.09f),
            size = Size(s * 0.82f, s * 0.82f),
            cornerRadius = CornerRadius(s * 0.17f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.075f),
            topLeft = Offset(s * 0.09f, s * 0.09f),
            size = Size(s * 0.82f, s * 0.82f),
            cornerRadius = CornerRadius(s * 0.17f),
            style = Stroke(width = s * 0.006f)
        )

        when (kind) {
            EmptyArtKind.MUSIC -> drawMusic(s, accent, secondaryAccent)
            EmptyArtKind.FOLDER -> drawFolder(s, accent, secondaryAccent)
            EmptyArtKind.SEARCH -> drawSearch(s, accent, secondaryAccent)
            EmptyArtKind.PLAYLIST -> drawPlaylist(s, accent, secondaryAccent)
            EmptyArtKind.EQUALIZER -> drawEqualizer(s, accent, secondaryAccent)
            EmptyArtKind.SLEEP -> drawSleep(s, accent, secondaryAccent)
        }
    }
}

private fun DrawScope.drawMusic(s: Float, accent: Color, secondary: Color) {
    val center = Offset(s * 0.45f, s * 0.53f)
    val radius = s * 0.23f

    // Plak gövdesi ve oluklar.
    drawCircle(
        brush = Brush.linearGradient(
            listOf(accent.copy(alpha = 0.55f), secondary.copy(alpha = 0.30f)),
            start = Offset(center.x - radius, center.y - radius),
            end = Offset(center.x + radius, center.y + radius)
        ),
        radius = radius,
        center = center
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.18f),
        radius = radius,
        center = center,
        style = Stroke(width = s * 0.008f)
    )
    listOf(0.80f, 0.60f).forEach { fraction ->
        drawCircle(
            color = Color.White.copy(alpha = 0.09f),
            radius = radius * fraction,
            center = center,
            style = Stroke(width = s * 0.006f)
        )
    }
    drawCircle(color = accent.copy(alpha = 0.95f), radius = radius * 0.30f, center = center)
    drawCircle(color = Color(0xFF07080F), radius = radius * 0.075f, center = center)

    // Sağ üstte çift nota.
    val leftHead = Offset(s * 0.66f, s * 0.36f)
    val rightHead = Offset(s * 0.78f, s * 0.32f)
    drawCircle(color = Color.White.copy(alpha = 0.88f), radius = s * 0.030f, center = leftHead)
    drawCircle(color = Color.White.copy(alpha = 0.88f), radius = s * 0.030f, center = rightHead)
    drawLine(
        color = Color.White.copy(alpha = 0.88f),
        start = Offset(leftHead.x + s * 0.022f, leftHead.y),
        end = Offset(leftHead.x + s * 0.022f, s * 0.22f),
        strokeWidth = s * 0.018f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color.White.copy(alpha = 0.88f),
        start = Offset(rightHead.x + s * 0.022f, rightHead.y),
        end = Offset(rightHead.x + s * 0.022f, s * 0.18f),
        strokeWidth = s * 0.018f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = accent.copy(alpha = 0.85f),
        start = Offset(leftHead.x + s * 0.022f, s * 0.22f),
        end = Offset(rightHead.x + s * 0.022f, s * 0.18f),
        strokeWidth = s * 0.020f,
        cap = StrokeCap.Round
    )

    drawSparkle(Offset(s * 0.27f, s * 0.30f), s * 0.035f, accent.copy(alpha = 0.75f))
    drawSparkle(Offset(s * 0.76f, s * 0.62f), s * 0.022f, Color.White.copy(alpha = 0.55f))
}

private fun DrawScope.drawFolder(s: Float, accent: Color, secondary: Color) {
    // Sekme ve gövde.
    drawRoundRect(
        brush = Brush.linearGradient(listOf(accent.copy(alpha = 0.30f), accent.copy(alpha = 0.16f))),
        topLeft = Offset(s * 0.20f, s * 0.28f),
        size = Size(s * 0.34f, s * 0.16f),
        cornerRadius = CornerRadius(s * 0.055f)
    )
    drawRoundRect(
        brush = Brush.linearGradient(
            listOf(accent.copy(alpha = 0.34f), secondary.copy(alpha = 0.14f)),
            start = Offset(s * 0.18f, s * 0.34f),
            end = Offset(s * 0.82f, s * 0.74f)
        ),
        topLeft = Offset(s * 0.18f, s * 0.34f),
        size = Size(s * 0.64f, s * 0.40f),
        cornerRadius = CornerRadius(s * 0.075f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.16f),
        topLeft = Offset(s * 0.18f, s * 0.34f),
        size = Size(s * 0.64f, s * 0.40f),
        cornerRadius = CornerRadius(s * 0.075f),
        style = Stroke(width = s * 0.007f)
    )

    // Ön kapak ve içindeki şarkı satırları.
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.11f), Color.White.copy(alpha = 0.035f))
        ),
        topLeft = Offset(s * 0.18f, s * 0.43f),
        size = Size(s * 0.64f, s * 0.31f),
        cornerRadius = CornerRadius(s * 0.070f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.10f),
        topLeft = Offset(s * 0.18f, s * 0.43f),
        size = Size(s * 0.64f, s * 0.31f),
        cornerRadius = CornerRadius(s * 0.070f),
        style = Stroke(width = s * 0.006f)
    )
    val lineAlphas = listOf(0.55f, 0.34f, 0.42f)
    lineAlphas.forEachIndexed { index, alpha ->
        drawRoundRect(
            color = accent.copy(alpha = alpha),
            topLeft = Offset(s * 0.29f, s * (0.51f + index * 0.07f)),
            size = Size(s * (0.38f - index * 0.05f), s * 0.018f),
            cornerRadius = CornerRadius(s * 0.009f)
        )
    }
    drawCircle(
        color = secondary.copy(alpha = 0.75f),
        radius = s * 0.024f,
        center = Offset(s * 0.72f, s * 0.60f)
    )

    drawSparkle(Offset(s * 0.77f, s * 0.27f), s * 0.030f, Color.White.copy(alpha = 0.65f))
}

private fun DrawScope.drawSearch(s: Float, accent: Color, secondary: Color) {
    val ringCenter = Offset(s * 0.45f, s * 0.44f)
    val ringRadius = s * 0.19f

    drawCircle(
        brush = Brush.linearGradient(
            listOf(accent, secondary.copy(alpha = 0.75f)),
            start = Offset(ringCenter.x - ringRadius, ringCenter.y - ringRadius),
            end = Offset(ringCenter.x + ringRadius, ringCenter.y + ringRadius)
        ),
        radius = ringRadius,
        center = ringCenter,
        style = Stroke(width = s * 0.042f)
    )
    drawLine(
        brush = Brush.linearGradient(
            listOf(accent, secondary.copy(alpha = 0.8f)),
            start = Offset(s * 0.60f, s * 0.60f),
            end = Offset(s * 0.77f, s * 0.77f)
        ),
        start = Offset(s * 0.59f, s * 0.59f),
        end = Offset(s * 0.77f, s * 0.77f),
        strokeWidth = s * 0.052f,
        cap = StrokeCap.Round
    )

    // Büyütecin içinde küçük bir ekolayzer.
    val barHeights = listOf(0.075f, 0.13f, 0.095f)
    barHeights.forEachIndexed { index, height ->
        drawRoundRect(
            color = accent.copy(alpha = 0.95f),
            topLeft = Offset(s * (0.385f + index * 0.052f), s * (0.50f - height)),
            size = Size(s * 0.030f, s * height),
            cornerRadius = CornerRadius(s * 0.015f)
        )
    }

    drawSparkle(Offset(s * 0.72f, s * 0.27f), s * 0.032f, Color.White.copy(alpha = 0.70f))
    drawSparkle(Offset(s * 0.28f, s * 0.66f), s * 0.022f, accent.copy(alpha = 0.65f))
}

private fun DrawScope.drawPlaylist(s: Float, accent: Color, secondary: Color) {
    val cardSize = Size(s * 0.50f, s * 0.46f)
    val cardTopLeft = Offset(s * 0.25f, s * 0.27f)

    rotate(degrees = -12f, pivot = Offset(s * 0.5f, s * 0.5f)) {
        drawRoundRect(
            color = secondary.copy(alpha = 0.14f),
            topLeft = cardTopLeft,
            size = cardSize,
            cornerRadius = CornerRadius(s * 0.09f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.07f),
            topLeft = cardTopLeft,
            size = cardSize,
            cornerRadius = CornerRadius(s * 0.09f),
            style = Stroke(width = s * 0.006f)
        )
    }
    rotate(degrees = 9f, pivot = Offset(s * 0.5f, s * 0.5f)) {
        drawRoundRect(
            color = accent.copy(alpha = 0.16f),
            topLeft = cardTopLeft,
            size = cardSize,
            cornerRadius = CornerRadius(s * 0.09f)
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = cardTopLeft,
            size = cardSize,
            cornerRadius = CornerRadius(s * 0.09f),
            style = Stroke(width = s * 0.006f)
        )
    }

    // Ön kart.
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.13f), Color.White.copy(alpha = 0.035f))
        ),
        topLeft = cardTopLeft,
        size = cardSize,
        cornerRadius = CornerRadius(s * 0.09f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = cardTopLeft,
        size = cardSize,
        cornerRadius = CornerRadius(s * 0.09f),
        style = Stroke(width = s * 0.007f)
    )

    // Kart içeriği: üç satır ve oynat düğmesi.
    listOf(0.42f, 0.50f, 0.58f).forEachIndexed { index, y ->
        drawRoundRect(
            color = if (index == 0) accent.copy(alpha = 0.65f) else Color.White.copy(alpha = 0.24f),
            topLeft = Offset(s * 0.32f, s * y),
            size = Size(s * (0.26f - index * 0.035f), s * 0.020f),
            cornerRadius = CornerRadius(s * 0.010f)
        )
    }
    drawCircle(color = accent.copy(alpha = 0.9f), radius = s * 0.062f, center = Offset(s * 0.64f, s * 0.60f))
    val play = Path().apply {
        moveTo(s * 0.626f, s * 0.575f)
        lineTo(s * 0.626f, s * 0.625f)
        lineTo(s * 0.668f, s * 0.600f)
        close()
    }
    drawPath(play, color = Color(0xFF07080F).copy(alpha = 0.85f))

    drawSparkle(Offset(s * 0.77f, s * 0.29f), s * 0.030f, Color.White.copy(alpha = 0.70f))
}

private fun DrawScope.drawEqualizer(s: Float, accent: Color, secondary: Color) {
    drawRoundRect(
        color = Color.White.copy(alpha = 0.045f),
        topLeft = Offset(s * 0.22f, s * 0.25f),
        size = Size(s * 0.56f, s * 0.50f),
        cornerRadius = CornerRadius(s * 0.11f)
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.09f),
        topLeft = Offset(s * 0.22f, s * 0.25f),
        size = Size(s * 0.56f, s * 0.50f),
        cornerRadius = CornerRadius(s * 0.11f),
        style = Stroke(width = s * 0.006f)
    )

    val heights = listOf(0.16f, 0.28f, 0.20f, 0.34f, 0.23f)
    val baseY = s * 0.66f
    heights.forEachIndexed { index, height ->
        val x = s * (0.30f + index * 0.085f)
        val barHeight = s * height
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(
                    accent.copy(alpha = 0.95f),
                    secondary.copy(alpha = 0.55f)
                ),
                start = Offset(x, baseY - barHeight),
                end = Offset(x, baseY)
            ),
            topLeft = Offset(x, baseY - barHeight),
            size = Size(s * 0.045f, barHeight),
            cornerRadius = CornerRadius(s * 0.0225f)
        )
    }
    drawLine(
        color = Color.White.copy(alpha = 0.14f),
        start = Offset(s * 0.28f, baseY + s * 0.015f),
        end = Offset(s * 0.72f, baseY + s * 0.015f),
        strokeWidth = s * 0.008f,
        cap = StrokeCap.Round
    )

    drawSparkle(Offset(s * 0.75f, s * 0.22f), s * 0.026f, accent.copy(alpha = 0.70f))
}

private fun DrawScope.drawSleep(s: Float, accent: Color, secondary: Color) {
    // Hilal: iki dairenin farkı ile keskin bir ay yüzeyi.
    val outer = Path().apply {
        addOval(Rect(center = Offset(s * 0.43f, s * 0.47f), radius = s * 0.21f))
    }
    val inner = Path().apply {
        addOval(Rect(center = Offset(s * 0.52f, s * 0.41f), radius = s * 0.19f))
    }
    val crescent = Path().apply { op(outer, inner, PathOperation.Difference) }
    drawPath(
        path = crescent,
        brush = Brush.linearGradient(
            listOf(accent.copy(alpha = 0.95f), secondary.copy(alpha = 0.75f)),
            start = Offset(s * 0.22f, s * 0.28f),
            end = Offset(s * 0.55f, s * 0.66f)
        )
    )

    drawSparkle(Offset(s * 0.67f, s * 0.28f), s * 0.040f, Color.White.copy(alpha = 0.80f))
    drawSparkle(Offset(s * 0.76f, s * 0.42f), s * 0.024f, accent.copy(alpha = 0.70f))
    drawSparkle(Offset(s * 0.27f, s * 0.31f), s * 0.022f, Color.White.copy(alpha = 0.50f))

    // Uyku "Z"leri.
    drawZ(Offset(s * 0.62f, s * 0.62f), s * 0.075f, Color.White.copy(alpha = 0.72f), s * 0.012f)
    drawZ(Offset(s * 0.71f, s * 0.53f), s * 0.058f, Color.White.copy(alpha = 0.50f), s * 0.010f)
    drawZ(Offset(s * 0.78f, s * 0.46f), s * 0.042f, Color.White.copy(alpha = 0.34f), s * 0.009f)
}

private fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val stroke = (radius * 0.30f).coerceAtLeast(0.6f)
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawZ(center: Offset, size: Float, color: Color, stroke: Float) {
    val half = size / 2f
    drawLine(
        color = color,
        start = Offset(center.x - half, center.y - half),
        end = Offset(center.x + half, center.y - half),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x + half, center.y - half),
        end = Offset(center.x - half, center.y + half),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(center.x - half, center.y + half),
        end = Offset(center.x + half, center.y + half),
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
}
