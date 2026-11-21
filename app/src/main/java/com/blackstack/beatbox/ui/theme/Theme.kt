/*
 * BeatBox Project (C) 2026
 *
 * Tema üretimi Metrolist projesinden (GPL-3.0) uyarlanmıştır.
 * Kaynak: shared/src/commonMain/kotlin/com/metrolist/shared/ui/theme/CommonTheme.kt
 */

package com.blackstack.beatbox.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import me.tatarka.google.material.dynamiccolor.DynamicScheme
import me.tatarka.google.material.hct.Hct
import me.tatarka.google.material.scheme.SchemeContent
import me.tatarka.google.material.scheme.SchemeExpressive
import me.tatarka.google.material.scheme.SchemeFruitSalad
import me.tatarka.google.material.scheme.SchemeMonochrome
import me.tatarka.google.material.scheme.SchemeNeutral
import me.tatarka.google.material.scheme.SchemeRainbow
import me.tatarka.google.material.scheme.SchemeTonalSpot
import me.tatarka.google.material.scheme.SchemeVibrant

/**
 * BeatBox'in tek tema giriş noktası.
 *
 * Renk şeması [ThemeConfig]'e göre üretilir. Material You açıkken sistem
 * duvar kağıdı renkleri, albüm kapağı renkleri açıkken çalan şarkının kapağı
 * tohum renk olarak kullanılır. [ThemeMode.PURE_BLACK] seçildiğinde tüm
 * yüzeyler saf siyaha çekilir.
 */
@Composable
fun BeatBoxTheme(
    config: ThemeConfig,
    artworkSeedColor: Color? = null,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = config.isDark(systemDark)
    val pureBlack = config.isPureBlack(systemDark)

    val baseScheme = remember(config, artworkSeedColor, darkTheme, context) {
        buildBeatBoxColorScheme(config, artworkSeedColor, darkTheme, context)
    }
    val colorScheme = remember(baseScheme, pureBlack) {
        if (pureBlack) baseScheme.pureBlack() else baseScheme
    }
    val animatedScheme = animateColorScheme(colorScheme, config.uiEnhancements)

    CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
        MaterialTheme(
            colorScheme = animatedScheme,
            typography = BeatBoxTypography,
            shapes = BeatBoxShapes,
            content = content,
        )
    }
}

/**
 * Saf fonksiyon: tema yapılandırması + isteğe bağlı albüm rengi -> renk şeması.
 *
 * Material You açıkken ve TONAL_SPOT paletinde sistemin dinamik renkleri
 * kullanılır. Diğer palet stillerinde duvar kağıdı vurgu renginden şema
 * üretilir. Material You kapalıyken seçilen tohum renk kullanılır.
 */
fun buildBeatBoxColorScheme(
    config: ThemeConfig,
    artworkSeedColor: Color?,
    darkTheme: Boolean,
    context: Context? = null,
): ColorScheme {
    val useSystemDynamic = config.materialYou &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
        artworkSeedColor == null

    if (useSystemDynamic && context != null) {
        if (config.paletteStyle == ThemePaletteStyle.TONAL_SPOT) {
            return if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
        }
        val wallpaperSeed = wallpaperSeedColor(context)
        return generateColorScheme(wallpaperSeed, darkTheme, config.paletteStyle)
    }

    val seed = resolveThemeSeedColor(config, artworkSeedColor)
    return generateColorScheme(seed, darkTheme, config.paletteStyle)
}

/** Material Color Utilities ile seçilen stilde renk şeması üretir. */
fun generateColorScheme(
    seedColor: Color,
    darkTheme: Boolean,
    paletteStyle: ThemePaletteStyle,
    contrastLevel: Double = 0.0,
): ColorScheme {
    val seed = seedColor.toArgb()
    val hct = Hct.fromInt(seed)
    val scheme: DynamicScheme = when (paletteStyle) {
        ThemePaletteStyle.TONAL_SPOT -> SchemeTonalSpot(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.VIBRANT -> SchemeVibrant(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.EXPRESSIVE -> SchemeExpressive(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.RAINBOW -> SchemeRainbow(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.FRUIT_SALAD -> SchemeFruitSalad(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.MONOCHROME -> SchemeMonochrome(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.NEUTRAL -> SchemeNeutral(hct, darkTheme, contrastLevel)
        ThemePaletteStyle.CONTENT -> SchemeContent(hct, darkTheme, contrastLevel)
    }
    return scheme.toColorScheme()
}

/** Saf siyah modu; Metrolist'in `pureBlack` uzantısıyla aynıdır. */
fun ColorScheme.pureBlack(): ColorScheme = copy(
    surface = Color.Black,
    background = Color.Black,
    surfaceDim = Color.Black,
    surfaceBright = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color.Black,
    surfaceContainer = Color.Black,
    surfaceContainerHigh = Color.Black,
    surfaceContainerHighest = Color.Black,
    surfaceVariant = Color.Black,
)

private fun DynamicScheme.toColorScheme(): ColorScheme {
    // Tüm rolleri açıkça verdiğimiz için lightColorScheme ile darkColorScheme
    // aynı nesneyi üretir; tek çağrı yeterlidir.
    return lightColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(surfaceTint),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        primaryFixed = Color(primaryFixed),
        primaryFixedDim = Color(primaryFixedDim),
        onPrimaryFixed = Color(onPrimaryFixed),
        onPrimaryFixedVariant = Color(onPrimaryFixedVariant),
        secondaryFixed = Color(secondaryFixed),
        secondaryFixedDim = Color(secondaryFixedDim),
        onSecondaryFixed = Color(onSecondaryFixed),
        onSecondaryFixedVariant = Color(onSecondaryFixedVariant),
        tertiaryFixed = Color(tertiaryFixed),
        tertiaryFixedDim = Color(tertiaryFixedDim),
        onTertiaryFixed = Color(onTertiaryFixed),
        onTertiaryFixedVariant = Color(onTertiaryFixedVariant),
    )
}

private fun wallpaperSeedColor(context: Context): Color {
    val id = context.resources.getIdentifier("system_accent1_500", "color", "android")
    return if (id != 0) {
        Color(ContextCompat.getColor(context, id))
    } else {
        DefaultThemeSeedColor
    }
}
