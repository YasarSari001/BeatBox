package com.blackstack.beatbox.desktop

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/*
 * Masaüstü teması.
 *
 * Android sürümündeki Aurora Glass paletini (bkz. app/.../ui/theme/Color.kt)
 * temel alır; ayarlardan mod ve vurgu rengi değiştirilebilir.
 */

enum class DesktopThemeMode { DARK, LIGHT, AMOLED, PURE_BLACK }

data class AccentOption(val label: String, val color: Color)

/** Android'deki tema tohum renklerine karşılık gelen hazır palet. */
val AccentPalette = listOf(
    AccentOption("Aurora", Color(0xFFA855F7)),
    AccentOption("Neon", Color(0xFF22D3EE)),
    AccentOption("Pink", Color(0xFFF472B6)),
    AccentOption("Mint", Color(0xFF4ADE80)),
    AccentOption("Amber", Color(0xFFFBBF24)),
    AccentOption("Coral", Color(0xFFFB7185)),
)

data class DesktopTheme(
    val mode: DesktopThemeMode = DesktopThemeMode.DARK,
    val accentIndex: Int = 0,
) {
    val accent: Color get() = AccentPalette[accentIndex.coerceIn(0, AccentPalette.lastIndex)].color
}

/** Cam yüzeylerde kullanılan beyaz katman oranları (Android ile aynı). */
const val GlassAlphaLow = 0.06f
const val GlassAlphaMedium = 0.10f
const val GlassAlphaHigh = 0.16f

private val AuroraCyan = Color(0xFF22D3EE)

@Suppress("FunctionName")
fun DesktopColorScheme(theme: DesktopTheme): ColorScheme {
    val accent = theme.accent
    return when (theme.mode) {
        DesktopThemeMode.LIGHT -> lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = accent.copy(alpha = 0.16f),
            onPrimaryContainer = Color(0xFF14141C),
            secondary = AuroraCyan,
            onSecondary = Color(0xFF04212A),
            background = Color(0xFFF5F5FA),
            onBackground = Color(0xFF14141C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF14141C),
            surfaceVariant = Color(0xFFECECF4),
            onSurfaceVariant = Color(0xFF54566B),
            outline = Color(0xFFB9BAC9),
            error = Color(0xFFB3261E),
            onError = Color.White,
        )

        DesktopThemeMode.AMOLED -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0A0A0A),
            primaryContainer = accent.copy(alpha = 0.24f),
            onPrimaryContainer = Color(0xFFF7F8FC),
            secondary = AuroraCyan,
            onSecondary = Color(0xFF0A0A0A),
            background = Color(0xFF000000),
            onBackground = Color(0xFFF7F8FC),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFF7F8FC),
            surfaceVariant = Color(0xFF15161C),
            onSurfaceVariant = Color(0xFFA7ADBE),
            outline = Color(0xFF3A3D4A),
            error = Color(0xFFFB7185),
            onError = Color(0xFF1A0207),
        )

        DesktopThemeMode.PURE_BLACK -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF000000),
            primaryContainer = accent.copy(alpha = 0.20f),
            onPrimaryContainer = Color(0xFFFFFFFF),
            secondary = AuroraCyan,
            onSecondary = Color(0xFF000000),
            background = Color(0xFF000000),
            onBackground = Color(0xFFFFFFFF),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFF0A0A0A),
            onSurfaceVariant = Color(0xFFB0B0B0),
            outline = Color(0xFF2A2A2A),
            error = Color(0xFFFF6B6B),
            onError = Color(0xFF000000),
        )

        DesktopThemeMode.DARK -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF0B0712),
            primaryContainer = accent.copy(alpha = 0.22f),
            onPrimaryContainer = Color(0xFFF7F8FC),
            secondary = AuroraCyan,
            onSecondary = Color(0xFF06222B),
            background = Color(0xFF07080F),
            onBackground = Color(0xFFF7F8FC),
            surface = Color(0xFF10121C),
            onSurface = Color(0xFFF7F8FC),
            surfaceVariant = Color(0xFF1E2230),
            onSurfaceVariant = Color(0xFFA7ADBE),
            outline = Color(0xFF3A3F52),
            error = Color(0xFFFB7185),
            onError = Color(0xFF1A0207),
        )
    }
}

/** Pencerenin arka planında kullanılan dikey degrade. */
fun DesktopBackgroundGradient(theme: DesktopTheme): List<Color> = when (theme.mode) {
    DesktopThemeMode.LIGHT -> listOf(Color(0xFFF7F7FC), Color(0xFFEDEBF7), Color(0xFFF7F4FB))
    DesktopThemeMode.AMOLED -> listOf(Color(0xFF000000), Color(0xFF050308), Color(0xFF000000))
    DesktopThemeMode.PURE_BLACK -> listOf(Color(0xFF000000), Color(0xFF000000), Color(0xFF000000))
    DesktopThemeMode.DARK -> listOf(Color(0xFF07080F), Color(0xFF120F1D), Color(0xFF08060D))
}
