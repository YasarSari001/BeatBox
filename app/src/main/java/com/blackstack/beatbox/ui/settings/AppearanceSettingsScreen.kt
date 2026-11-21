/*
 * BeatBox Project (C) 2026
 *
 * Metrolist projesinin görünüm ayarlarından (GPL-3.0) uyarlanmıştır.
 * Kaynak: shared/src/commonMain/kotlin/com/metrolist/shared/ui/settings/AppearanceSettingsRegistration.kt
 */

package com.blackstack.beatbox.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.theme.ThemeConfig
import com.blackstack.beatbox.ui.theme.ThemeMode
import com.blackstack.beatbox.ui.theme.ThemePaletteStyle
import com.blackstack.beatbox.ui.theme.ThemeSeedPresets
import com.blackstack.beatbox.ui.theme.generateColorScheme

/**
 * Metrolist tarzı "Görünüm" alt ekranı.
 *
 * Tema modu, Material You, albüm kapağı renkleri, tohum renk ve palet stili
 * burada yönetilir. Tüm değişiklikler anında uygulanır.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppearanceSettingsScreen(
    strings: Strings,
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onBack: () -> Unit,
) {
    var showModeDialog by remember { mutableStateOf(false) }
    var showPaletteDialog by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding()) {
            TopAppBar(
                title = { Text(strings.get("appearance_title"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.get("back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(Color.Transparent),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            ) {
                item {
                    SettingsGroupCard(title = strings.get("appearance_theme")) {
                        ThemeModeRow(
                            strings = strings,
                            mode = themeConfig.mode,
                            onClick = { showModeDialog = true },
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.Brightness6,
                            title = strings.get("material_you"),
                            subtitle = strings.get("material_you_desc"),
                            checked = themeConfig.materialYou,
                            onCheckedChange = { enabled ->
                                onThemeConfigChange(
                                    themeConfig.copy(
                                        materialYou = enabled,
                                        dynamicTheme = if (enabled) themeConfig.dynamicTheme else false,
                                    )
                                )
                            },
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = strings.get("ui_enhancements"),
                            subtitle = strings.get("ui_enhancements_desc"),
                            checked = themeConfig.uiEnhancements,
                            onCheckedChange = { onThemeConfigChange(themeConfig.copy(uiEnhancements = it)) },
                        )
                        SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.Palette,
                            title = strings.get("song_artwork_colors"),
                            subtitle = strings.get("song_artwork_colors_desc"),
                            checked = themeConfig.dynamicTheme,
                            enabled = themeConfig.materialYou,
                            onCheckedChange = { onThemeConfigChange(themeConfig.copy(dynamicTheme = it)) },
                        )
                        SettingsDivider()
                        PaletteStyleRow(
                            strings = strings,
                            style = themeConfig.paletteStyle,
                            onClick = { showPaletteDialog = true },
                        )
                    }
                }

                if (!themeConfig.materialYou) {
                    item {
                        ThemeColorCard(
                            strings = strings,
                            themeConfig = themeConfig,
                            onThemeConfigChange = onThemeConfigChange,
                        )
                    }
                }

                item {
                    PalettePreviewCard(strings = strings, themeConfig = themeConfig)
                }
            }
        }
    }

    if (showModeDialog) {
        val choices = listOf(
            SettingsChoice(ThemeMode.AUTO.name, strings.get("theme_mode_auto")) { ThemeModePreview(ThemeMode.AUTO) },
            SettingsChoice(ThemeMode.LIGHT.name, strings.get("theme_mode_light")) { ThemeModePreview(ThemeMode.LIGHT) },
            SettingsChoice(ThemeMode.DARK.name, strings.get("theme_mode_dark")) { ThemeModePreview(ThemeMode.DARK) },
            SettingsChoice(ThemeMode.PURE_BLACK.name, strings.get("pure_black")) { ThemeModePreview(ThemeMode.PURE_BLACK) },
        )
        SettingsChoiceDialog(
            title = strings.get("theme_mode"),
            choices = choices,
            selectedId = themeConfig.mode.name,
            strings = strings,
            onSelect = { id ->
                showModeDialog = false
                onThemeConfigChange(themeConfig.copy(mode = ThemeMode.valueOf(id)))
            },
            onDismiss = { showModeDialog = false },
        )
    }

    if (showPaletteDialog) {
        val choices = ThemePaletteStyle.entries.map { style ->
            SettingsChoice(
                id = style.name,
                label = strings.get(paletteStyleKey(style)),
                preview = { PaletteStylePreview(style, themeConfig.selectedThemeColor) },
            )
        }
        SettingsChoiceDialog(
            title = strings.get("palette_style"),
            choices = choices,
            selectedId = themeConfig.paletteStyle.name,
            strings = strings,
            onSelect = { id ->
                showPaletteDialog = false
                onThemeConfigChange(themeConfig.copy(paletteStyle = ThemePaletteStyle.valueOf(id)))
            },
            onDismiss = { showPaletteDialog = false },
        )
    }
}

@Composable
private fun ThemeModeRow(
    strings: Strings,
    mode: ThemeMode,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Brightness6,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.get("theme_mode"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = themeModeLabel(strings, mode),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ThemeModePreview(mode)
        Spacer(Modifier.width(4.dp))
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun PaletteStyleRow(
    strings: Strings,
    style: ThemePaletteStyle,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.ColorLens,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.get("palette_style"),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = strings.get(paletteStyleKey(style)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(22.dp),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemeColorCard(
    strings: Strings,
    themeConfig: ThemeConfig,
    onThemeConfigChange: (ThemeConfig) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val selected = themeConfig.selectedThemeColor
    var hex by remember(selected) { mutableStateOf(selected.toHexRgb()) }

    SettingsGroupCard(title = strings.get("theme_color")) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = strings.get("theme_color_desc"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ThemeSeedPresets.forEach { color ->
                    val isSelected = color.toHexRgb() == selected.toHexRgb()
                    val scheme = remember(color, isDark, themeConfig.paletteStyle) {
                        generateColorScheme(color, isDark, themeConfig.paletteStyle)
                    }
                    Surface(
                        modifier = Modifier
                            .size(52.dp)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                                shape = CircleShape,
                            ),
                        shape = CircleShape,
                        color = scheme.surfaceContainerHighest,
                        onClick = {
                            hex = color.toHexRgb()
                            onThemeConfigChange(themeConfig.copy(selectedThemeColor = color))
                            focusManager.clearFocus()
                        },
                    ) {
                        Canvas(Modifier.fillMaxSize().padding(if (isSelected) 7.dp else 5.dp)) {
                            drawArc(scheme.primary, 180f, 180f, useCenter = true)
                            drawArc(scheme.secondary, 90f, 90f, useCenter = true)
                            drawArc(scheme.tertiary, 0f, 90f, useCenter = true)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = hex,
                onValueChange = { input ->
                    val normalized = input
                        .filter { character -> character in '0'..'9' || character.lowercaseChar() in 'a'..'f' }
                        .take(6)
                        .uppercase()
                    hex = normalized
                    if (normalized.length == 6) {
                        normalized.toLongOrNull(16)?.let { rgb ->
                            onThemeConfigChange(
                                themeConfig.copy(selectedThemeColor = Color(0xFF000000L or rgb))
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.get("color_hex")) },
                prefix = { Text("#") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        hex.toLongOrNull(16)?.takeIf { hex.length == 6 }?.let { rgb ->
                            onThemeConfigChange(
                                themeConfig.copy(selectedThemeColor = Color(0xFF000000L or rgb))
                            )
                        }
                        focusManager.clearFocus()
                    }
                ),
                trailingIcon = {
                    if (hex.length == 6) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PalettePreviewCard(
    strings: Strings,
    themeConfig: ThemeConfig,
) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = remember(themeConfig, isDark) {
        generateColorScheme(themeConfig.selectedThemeColor, isDark, themeConfig.paletteStyle)
    }
    SettingsGroupCard(title = strings.get("theme_preview")) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(46.dp)
                        .background(scheme.primaryContainer, RoundedCornerShape(12.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = strings.get("theme_preview_title"),
                        color = scheme.onSurface,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = strings.get("theme_preview_subtitle"),
                        color = scheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Box(
                    Modifier
                        .size(38.dp)
                        .background(scheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                listOf(
                    scheme.primary to "P",
                    scheme.secondary to "S",
                    scheme.tertiary to "T",
                    scheme.surfaceContainerHighest to "S",
                    scheme.onSurface to "O",
                ).forEach { (color, label) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                                .background(color, RoundedCornerShape(10.dp))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                    RoundedCornerShape(10.dp),
                                )
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaletteStylePreview(style: ThemePaletteStyle, seedColor: Color) {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val scheme = remember(style, seedColor, isDark) {
        generateColorScheme(seedColor, isDark, style)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(scheme.primary, scheme.secondary, scheme.tertiary, scheme.primaryContainer).forEach { color ->
            Box(Modifier.size(14.dp).background(color, CircleShape))
        }
    }
}

private fun paletteStyleKey(style: ThemePaletteStyle): String = when (style) {
    ThemePaletteStyle.TONAL_SPOT -> "palette_tonal_spot"
    ThemePaletteStyle.VIBRANT -> "palette_vibrant"
    ThemePaletteStyle.EXPRESSIVE -> "palette_expressive"
    ThemePaletteStyle.RAINBOW -> "palette_rainbow"
    ThemePaletteStyle.FRUIT_SALAD -> "palette_fruit_salad"
    ThemePaletteStyle.MONOCHROME -> "palette_monochrome"
    ThemePaletteStyle.NEUTRAL -> "palette_neutral"
    ThemePaletteStyle.CONTENT -> "palette_content"
}

private fun Color.toHexRgb(): String {
    if (red.isNaN() || green.isNaN() || blue.isNaN()) return "6750A4"
    val r = (red.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0')
    val g = (green.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0')
    val b = (blue.coerceIn(0f, 1f) * 255f).toInt().toString(16).padStart(2, '0')
    return "$r$g$b".uppercase()
}
