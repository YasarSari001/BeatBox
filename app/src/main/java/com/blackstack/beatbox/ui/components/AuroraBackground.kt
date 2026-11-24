package com.blackstack.beatbox.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.random.Random

/**
 * Aurora Glass tuvali.
 *
 * Seçili renk şemasının arka planı üzerine albümden türeyen
 * birincil/ikincil renklerden iki yumuşak ışık lekesi ve çok hafif bir grain
 * ekler. Işık lekeleri yavaşça sürüklenir; reduced-motion açıkken statik kalır.
 *
 * Performans modunda tam ekran gradyanlar her karede yeniden boyanmaz;
 * aynı kompozisyon bir kez küçük bir bitmap'e çizilip gerilerek gösterilir.
 * Görünüm korunurken kaydırma sırasındaki GPU yükü belirgin düşer.
 *
 * Eski `GradientBackground` ile aynı kullanım sözleşmesine sahiptir:
 * [modifier], [content] alanını sarar.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = LocalReducedMotion.current
    val performanceMode = rememberPerformanceMode()
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val background = MaterialTheme.colorScheme.background
    val surface = MaterialTheme.colorScheme.surface
    val isLight = background.luminance() > 0.5f

    val baseBrush = remember(background, surface) {
        Brush.verticalGradient(
            colors = listOf(background, surface, background),
            startY = 0f,
            endY = 3000f
        )
    }

    // Işık sürüklenmesi 26 saniyelik, neredeyse fark edilmeyen bir harekettir;
    // her karede katman güncellemek boşa kare harcar. Değer ~10 fps'te
    // ilerletilir, hareket aynı kalır ama boştaki çizim maliyeti düşer.
    val drift: State<Float> = if (reducedMotion || performanceMode) {
        remember { mutableStateOf(0f) }
    } else {
        produceState(initialValue = -1f, reducedMotion, performanceMode) {
            val periodNanos = 26_000_000_000L
            var startNanos = -1L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (startNanos < 0L) startNanos = frameTimeNanos
                    val elapsed = (frameTimeNanos - startNanos) % (periodNanos * 2)
                    val phase = if (elapsed < periodNanos) {
                        elapsed.toFloat() / periodNanos
                    } else {
                        2f - elapsed.toFloat() / periodNanos
                    }
                    val next = -1f + 2f * phase
                    if (abs(next - value) >= 0.008f) value = next
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(baseBrush)
    ) {
        if (performanceMode) {
            StaticAuroraLayer(
                primary = primary,
                secondary = secondary,
                tertiary = tertiary,
                background = background,
                surface = surface,
                isLight = isLight,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val glowSize = maxWidth * 1.35f
            val travel = maxWidth * 0.10f

            // Üst-sol: birincil renk.
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = -maxWidth * 0.55f, y = -maxWidth * 0.48f)
                    .size(glowSize)
                    .graphicsLayer {
                        translationX = travel.toPx() * drift.value
                        translationY = travel.toPx() * drift.value * 0.55f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.30f),
                                primary.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Alt-sağ: ikincil renk.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = maxWidth * 0.42f, y = maxWidth * 0.40f)
                    .size(glowSize)
                    .graphicsLayer {
                        translationX = -travel.toPx() * drift.value * 0.8f
                        translationY = -travel.toPx() * drift.value * 0.6f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                secondary.copy(alpha = 0.24f),
                                secondary.copy(alpha = 0.08f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Orta-alt: üçüncül renk, kompozisyonu dengeler.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .offset(x = -maxWidth * 0.35f, y = maxWidth * 0.72f)
                    .size(maxWidth * 1.1f)
                    .graphicsLayer {
                        translationX = travel.toPx() * drift.value * 0.45f
                        translationY = -travel.toPx() * drift.value * 0.30f
                    }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                tertiary.copy(alpha = 0.14f),
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
            )

            // Grain: deterministik noktalar, animasyona katılmaz.
            val grain = remember {
                val random = Random(0xBE47)
                List(1_100) { Offset(random.nextFloat(), random.nextFloat()) }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Kendi katmanına alınır: 1100 nokta bir kez raster edilir,
                    // üstteki içerik yeniden çizilse bile tekrar çizilmez.
                    .graphicsLayer()
                    .drawWithCache {
                        val points = grain.map { Offset(it.x * size.width, it.y * size.height) }
                        onDrawBehind {
                            drawPoints(
                                points = points,
                                pointMode = PointMode.Points,
                                color = if (isLight) {
                                    Color.Black.copy(alpha = 0.02f)
                                } else {
                                    Color.White.copy(alpha = 0.028f)
                                },
                                strokeWidth = 1f
                            )
                        }
                    }
            )
        }

        // Kenarlarda hafif karartma; ortadaki içerik daha okunaklı olur.
        // Performans modunda bu katman bitmap'e dahil edilir.
        if (!performanceMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = if (isLight) 0.04f else 0.22f),
                            0.18f to Color.Transparent,
                            0.78f to Color.Transparent,
                            1f to Color.Black.copy(alpha = if (isLight) 0.08f else 0.42f)
                        )
                    )
            )
        }

        content()
    }
}

/**
 * Performans modu için statik aurora.
 *
 * Aynı renkleri ve konumları kullanır ama üç tam ekran gradyanı her karede
 * GPU'da yeniden hesaplamak yerine küçük bir bitmap'e bir kez çizer.
 */
@Composable
private fun StaticAuroraLayer(
    primary: Color,
    secondary: Color,
    tertiary: Color,
    background: Color,
    surface: Color,
    isLight: Boolean,
    modifier: Modifier
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier = modifier) {
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        // Yumuşak gradyanlar için düşük çözünürlük yeterli; bellek ve çizim
        // maliyeti düşer.
        val targetWidth = (widthPx * 0.5f).toInt().coerceIn(1, 720)
        val scale = if (widthPx > 0) targetWidth.toFloat() / widthPx else 1f
        val targetHeight =
            (heightPx * scale).toInt().coerceIn(1, 1600)

        val aurora by produceState<Bitmap?>(null, targetWidth, targetHeight, primary, secondary, tertiary, background, surface, scale) {
            value = withContext(Dispatchers.Default) {
                renderStaticAurora(
                    widthPx = targetWidth,
                    heightPx = targetHeight,
                    background = background.toArgb(),
                    surface = surface.toArgb(),
                    primary = primary.toArgb(),
                    secondary = secondary.toArgb(),
                    tertiary = tertiary.toArgb(),
                    isLight = isLight,
                    gradientEndScale = scale
                )
            }
        }

        aurora?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                filterQuality = FilterQuality.Low,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun renderStaticAurora(
    widthPx: Int,
    heightPx: Int,
    background: Int,
    surface: Int,
    primary: Int,
    secondary: Int,
    tertiary: Int,
    isLight: Boolean,
    gradientEndScale: Float,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.RGB_565)
    val canvas = Canvas(bitmap)
    val w = widthPx.toFloat()
    val h = heightPx.toFloat()

    val backgroundPaint = Paint().apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            3000f * gradientEndScale,
            intArrayOf(background, surface, background),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w, h, backgroundPaint)

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Üst-sol: birincil renk.
    glowPaint.shader = RadialGradient(
        w * 0.125f,
        w * 0.195f,
        w * 0.675f,
        intArrayOf(
            primary.withAlpha(0.30f),
            primary.withAlpha(0.10f),
            0
        ),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(w * 0.125f, w * 0.195f, w * 0.675f, glowPaint)

    // Alt-sağ: ikincil renk.
    glowPaint.shader = RadialGradient(
        w * 0.745f,
        h - w * 0.275f,
        w * 0.675f,
        intArrayOf(
            secondary.withAlpha(0.24f),
            secondary.withAlpha(0.08f),
            0
        ),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(w * 0.745f, h - w * 0.275f, w * 0.675f, glowPaint)

    // Orta-alt: üçüncül renk.
    glowPaint.shader = RadialGradient(
        w * 0.20f,
        h + w * 0.17f,
        w * 0.55f,
        intArrayOf(tertiary.withAlpha(0.14f), 0),
        null,
        Shader.TileMode.CLAMP
    )
    canvas.drawCircle(w * 0.20f, h + w * 0.17f, w * 0.55f, glowPaint)

    // Vinyet.
    val vignettePaint = Paint().apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            h,
            intArrayOf(
                if (isLight) 0x0A000000 else 0x38000000,
                0,
                0,
                if (isLight) 0x14000000 else 0x6B000000
            ),
            floatArrayOf(0f, 0.18f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, w, h, vignettePaint)

    return bitmap
}

private fun Int.withAlpha(alpha: Float): Int =
    (this and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24)
