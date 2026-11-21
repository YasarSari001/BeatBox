package com.blackstack.beatbox.ui.theme

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Albüm kapak görsellerinden renk paleti çıkaran yardımcı sınıf.
 *
 * Metrolist'in PlayerColorExtractor kavramından ilham alınmıştır ama
 * Material Kolor ve Coil 3 bağımlılığı kullanılmamıştır; sadece Android'in
 * yerleşik Bitmap işlemleri kullanılır.
 *
 * Performans için görsel seyrek örneklenir (en fazla ~40.000 piksel) ve
 * renkler 5 bitlik kanallara indirgenerek gruplanır. Böylece büyük kapak
 * görsellerinde bile ana iş parçacığı bloke edilmez.
 */
object PlayerColorExtractor {

    private const val MAX_SAMPLES = 40_000

    /**
     * Bir bitmap'ten baskın ve ikincil (accent) renkleri çıkarır.
     *
     * @param bitmap İşlenecek bitmap
     * @param fallbackColor Renk çıkarılamazsa kullanılan yedek renk
     * @param dominantOverride Palette gibi daha isabetli bir kaynaktan gelen
     *   baskın renk. Verilirse yeniden hesaplanan baskın renk yerine kullanılır.
     * @return PlayerArtworkColors nesnesi
     */
    fun extractColors(
        bitmap: Bitmap,
        fallbackColor: Color,
        dominantOverride: Color? = null
    ): PlayerArtworkColors {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0) {
            return fallbackPalette(dominantOverride ?: fallbackColor)
        }

        val step = sampleStep(width, height)
        val colorCounts = HashMap<Int, Int>(256)

        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = (pixel ushr 24) and 0xFF
                if (alpha >= 128) {
                    val r = (pixel shr 16) and 0xFF
                    val g = (pixel shr 8) and 0xFF
                    val b = pixel and 0xFF

                    // Neredeyse siyah/beyaz pikseller paleti bozmasın.
                    val luminance = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
                    if (luminance in 0.05f..0.95f) {
                        val key = ((r shr 3) shl 10) or ((g shr 3) shl 5) or (b shr 3)
                        colorCounts[key] = (colorCounts[key] ?: 0) + 1
                    }
                }
                x += step
            }
            y += step
        }

        if (colorCounts.isEmpty()) {
            return fallbackPalette(dominantOverride ?: fallbackColor)
        }

        val dominantBucket = colorCounts.maxByOrNull { it.value }!!.key
        val dominant = dominantOverride ?: bucketToColor(dominantBucket)
        val dominantHue = hueOf(dominant)

        var accent: Color? = null
        var accentCount = 0
        for ((key, count) in colorCounts) {
            if (count <= accentCount) continue
            val candidate = bucketToColor(key)
            val hueDiff = abs(hueOf(candidate) - dominantHue)
            val normalized = if (hueDiff > 180f) 360f - hueDiff else hueDiff
            if (normalized in 30f..150f) {
                accent = candidate
                accentCount = count
            }
        }

        val secondary = accent ?: dominant.copy(alpha = 0.85f)
        return PlayerArtworkColors(
            dominant = dominant,
            secondary = secondary,
            gradient = listOf(
                dominant,
                dominant.copy(alpha = 0.6f),
                Color(0xFF000000)
            ),
            textColor = if (luminanceOf(dominant) > 0.55f) Color(0xFF000000) else Color(0xFFFFFFFF)
        )
    }

    private fun sampleStep(width: Int, height: Int): Int {
        val total = width.toLong() * height.toLong()
        if (total <= MAX_SAMPLES) return 1
        return ceil(sqrt(total.toDouble() / MAX_SAMPLES)).toInt().coerceAtLeast(1)
    }

    /** 5 bitlik kanal anahtarını yaklaşık RGB rengine çevirir. */
    private fun bucketToColor(key: Int): Color {
        val r = ((key shr 10) and 0x1F) shl 3
        val g = ((key shr 5) and 0x1F) shl 3
        val b = (key and 0x1F) shl 3
        return Color(r, g, b)
    }

    private fun fallbackPalette(color: Color): PlayerArtworkColors = PlayerArtworkColors(
        dominant = color,
        secondary = color.copy(alpha = 0.85f),
        gradient = listOf(color, color.copy(alpha = 0.6f), Color(0xFF000000)),
        textColor = if (luminanceOf(color) > 0.55f) Color(0xFF000000) else Color(0xFFFFFFFF)
    )

    private fun hueOf(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        if (delta == 0f) return 0f
        val hue = if (max == r) {
            ((g - b) / delta).let { if (it < 0f) it + 6f else it }
        } else if (max == g) {
            (b - r) / delta + 2f
        } else {
            (r - g) / delta + 4f
        }
        return hue * 60f
    }

    private fun luminanceOf(color: Color): Float =
        0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
}

/**
 * Albüm kapağından çıkarılan renk paleti.
 */
data class PlayerArtworkColors(
    val dominant: Color,
    val secondary: Color,
    val gradient: List<Color>,
    val textColor: Color
)
