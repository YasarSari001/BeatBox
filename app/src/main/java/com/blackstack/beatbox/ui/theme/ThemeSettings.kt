/*
 * BeatBox Project (C) 2026
 *
 * Bu dosyadaki tema modeli Metrolist projesinden (GPL-3.0) uyarlanmıştır.
 * Kaynak: shared/src/commonMain/kotlin/com/metrolist/shared/settings/SettingEnums.kt
 */

package com.blackstack.beatbox.ui.theme

import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.edit

/** Metrolist'teki `ThemeMode` enum'unun BeatBox uyarlaması. */
enum class ThemeMode {
    AUTO,
    LIGHT,
    DARK,
    PURE_BLACK,
}

/** Metrolist'teki `ThemePaletteStyle` enum'unun BeatBox uyarlaması. */
enum class ThemePaletteStyle {
    TONAL_SPOT,
    VIBRANT,
    EXPRESSIVE,
    RAINBOW,
    FRUIT_SALAD,
    MONOCHROME,
    NEUTRAL,
    CONTENT,
}

/** Material Color Utilities'in varsayılan mor tohumu. */
val DefaultThemeSeedColor = Color(0xFF6750A4)

/** Metrolist ile aynı hazır renk paleti. */
val ThemeSeedPresets: List<Color> = listOf(
    Color(0xFF6750A4),
    Color(0xFF006A6A),
    Color(0xFF386A20),
    Color(0xFF8C4A60),
    Color(0xFF9C4146),
    Color(0xFF765A00),
)

/**
 * Uygulamanın tüm görünüm tercihleri.
 *
 * [materialYou] açıkken duvar kağıdı renkleri, [dynamicTheme] açıkken çalan
 * şarkının albüm kapağı tohum renk olarak kullanılır.
 */
data class ThemeConfig(
    val mode: ThemeMode = ThemeMode.AUTO,
    val materialYou: Boolean = true,
    val dynamicTheme: Boolean = true,
    val selectedThemeColor: Color = DefaultThemeSeedColor,
    val paletteStyle: ThemePaletteStyle = ThemePaletteStyle.TONAL_SPOT,
    val uiEnhancements: Boolean = true,
) {
    fun isDark(systemInDarkTheme: Boolean): Boolean = when (mode) {
        ThemeMode.AUTO -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.PURE_BLACK -> true
    }

    fun isPureBlack(systemInDarkTheme: Boolean): Boolean =
        mode == ThemeMode.PURE_BLACK && isDark(systemInDarkTheme)
}

/**
 * Tohum rengi seçimi; Metrolist'in `resolveThemeSeedColor` fonksiyonunun
 * birebir uyarlamasıdır.
 */
fun resolveThemeSeedColor(config: ThemeConfig, artworkSeedColor: Color?): Color = when {
    !config.materialYou -> config.selectedThemeColor
    config.dynamicTheme && artworkSeedColor != null -> artworkSeedColor
    else -> DefaultThemeSeedColor
}

/** Tema tercihlerinin SharedPreferences kalıcılığı ve eski temadan geçiş. */
object ThemePreferences {
    private const val KEY_MODE = "theme_mode"
    private const val KEY_MATERIAL_YOU = "theme_material_you"
    private const val KEY_DYNAMIC = "theme_dynamic"
    private const val KEY_SEED = "theme_seed_color"
    private const val KEY_PALETTE = "theme_palette_style"
    private const val KEY_UI = "theme_ui_enhancements"

    fun load(prefs: SharedPreferences): ThemeConfig {
        if (!prefs.contains(KEY_MODE)) {
            return migrateLegacy(prefs)
        }
        return ThemeConfig(
            mode = enumValue(prefs.getString(KEY_MODE, null), ThemeMode.AUTO),
            materialYou = prefs.getBoolean(KEY_MATERIAL_YOU, true),
            dynamicTheme = prefs.getBoolean(KEY_DYNAMIC, true),
            selectedThemeColor = Color(prefs.getInt(KEY_SEED, DefaultThemeSeedColor.toArgb())),
            paletteStyle = enumValue(prefs.getString(KEY_PALETTE, null), ThemePaletteStyle.TONAL_SPOT),
            uiEnhancements = prefs.getBoolean(KEY_UI, true),
        )
    }

    fun save(prefs: SharedPreferences, config: ThemeConfig) {
        prefs.edit {
            putString(KEY_MODE, config.mode.name)
            putBoolean(KEY_MATERIAL_YOU, config.materialYou)
            putBoolean(KEY_DYNAMIC, config.dynamicTheme)
            putInt(KEY_SEED, config.selectedThemeColor.toArgb())
            putString(KEY_PALETTE, config.paletteStyle.name)
            putBoolean(KEY_UI, config.uiEnhancements)
        }
    }

    /**
     * Eski `AppTheme` tercihini yeni modele taşır. DYNAMIC eski tema artık
     * Material You + albüm kapağı renkleri olarak devam eder.
     */
    private fun migrateLegacy(prefs: SharedPreferences): ThemeConfig {
        val legacy = prefs.getString("app_theme", null)
        val migrated = when (legacy) {
            "AMOLED" -> ThemeConfig(
                mode = ThemeMode.PURE_BLACK,
                materialYou = false,
                dynamicTheme = false,
            )
            "DARK" -> ThemeConfig(
                mode = ThemeMode.DARK,
                materialYou = false,
                dynamicTheme = false,
                selectedThemeColor = Color(0xFF9C27B0),
            )
            "NEON_CYBER" -> ThemeConfig(
                mode = ThemeMode.DARK,
                materialYou = false,
                dynamicTheme = false,
                selectedThemeColor = Color(0xFF00F5D4),
            )
            "ROSE_GOLD" -> ThemeConfig(
                mode = ThemeMode.DARK,
                materialYou = false,
                dynamicTheme = false,
                selectedThemeColor = Color(0xFFF2CC8F),
            )
            else -> ThemeConfig()
        }
        save(prefs, migrated)
        return migrated
    }

    private inline fun <reified T : Enum<T>> enumValue(name: String?, fallback: T): T =
        name?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: fallback
}
