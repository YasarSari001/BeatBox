package com.blackstack.beatbox.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Android 12 (API 31) ve üzeri için duvar kağıdından dinamik renk şeması üretir.
 *
 * Metrolist'in SystemDynamicColorScheme kavramından ilham alınmıştır ama
 * Material Kolor bağımlılığı kullanılmamıştır; sadece Material3'nin yerleşik
 * dynamicDarkColorScheme/dynamicLightColorScheme kullanılır.
 *
 * Samsung cihazlarda bazı sistem renk rolleri güncel olmayabilir; bu durumda
 * fallback olarak Material3'nin standart dinamik şeması kullanılır.
 */
@RequiresApi(Build.VERSION_CODES.S)
internal fun beatBoxDynamicColorScheme(
    context: Context,
    darkTheme: Boolean,
): ColorScheme {
    // Material3'nin yerleşik dinamik renk şeması kullanılır.
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

/**
 * API 31+'de dinamik renk desteklenmiyorsa null döner.
 */
fun isDynamicColorSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * Metin/rengin okunabilirliğini kontrol eder; gerekirse renk swap yapar.
 */
fun Color.isReadableOn(background: Color): Boolean {
    val thisLum = calculateLuminance(this)
    val bgLum = calculateLuminance(background)
    val contrast = (Math.max(thisLum, bgLum) + 0.05) / (Math.min(thisLum, bgLum) + 0.05)
    return contrast >= 4.5
}

/**
 * Renklerin parlaklık (luminance) değerini hesaplar.
 * Relative luminance formülü: 0.299*R + 0.587*G + 0.114*B
 */
fun calculateLuminance(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    return 0.299f * r + 0.587f * g + 0.114f * b
}

/**
 * Renkleri iki renk arasında interpolasyon yapar.
 */
fun Color.lerp(other: Color, t: Float): Color {
    val r = (red + (other.red - red) * t).coerceIn(0f, 1f)
    val g = (green + (other.green - green) * t).coerceIn(0f, 1f)
    val b = (blue + (other.blue - blue) * t).coerceIn(0f, 1f)
    val a = (alpha + (other.alpha - alpha) * t).coerceIn(0f, 1f)
    return Color(r, g, b, a)
}
