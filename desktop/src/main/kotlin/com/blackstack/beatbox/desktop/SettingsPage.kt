package com.blackstack.beatbox.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Minimize
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.EQPreset
import com.blackstack.beatbox.Strings
import java.io.File

/** Hakkında ekranındaki güncelleme denetiminin durumu. */
sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data object Failed : UpdateUiState
    data class Available(val update: DesktopUpdate) : UpdateUiState
}

/**
 * Masaüstü ayarları.
 *
 * Android'deki gruplanmış ayar ekranının masaüstü karşılığı: görünüm, ses,
 * kitaplık, zamanlayıcı, dil ve hakkında. Her satır gerçekten çalışır.
 */
@Composable
fun SettingsPage(
    strings: Strings,
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    theme: DesktopTheme,
    onThemeChange: (DesktopTheme) -> Unit,
    visualizerEnabled: Boolean,
    onVisualizerChange: (Boolean) -> Unit,
    closeToTray: Boolean,
    onCloseToTrayChange: (Boolean) -> Unit,
    crossfadeSeconds: Float,
    onCrossfadeChange: (Float) -> Unit,
    replayGain: Boolean,
    onReplayGainChange: (Boolean) -> Unit,
    eqPreset: EQPreset,
    onEqPresetChange: (EQPreset) -> Unit,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    folder: File?,
    trackCount: Int,
    totalSeconds: Double,
    onChooseFolder: () -> Unit,
    onRefresh: () -> Unit,
    sleepMinutes: Int,
    onSleepMinutes: (Int) -> Unit,
    sleepRemaining: Long?,
    updateState: UpdateUiState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        SettingsGroup(strings.dt("appearance")) {
            SettingsRow(
                icon = Icons.Rounded.Palette,
                title = strings.dt("theme_mode"),
                subtitle = when (theme.mode) {
                    DesktopThemeMode.DARK -> strings.dt("theme_dark")
                    DesktopThemeMode.LIGHT -> strings.dt("theme_light")
                    DesktopThemeMode.AMOLED -> strings.dt("theme_amoled")
                    DesktopThemeMode.PURE_BLACK -> strings.dt("theme_amoled")
                },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopThemeMode.values().forEach { mode ->
                        Chip(
                            text = when (mode) {
                                DesktopThemeMode.DARK -> strings.dt("theme_dark")
                                DesktopThemeMode.LIGHT -> strings.dt("theme_light")
                                DesktopThemeMode.AMOLED -> strings.dt("theme_amoled")
                                DesktopThemeMode.PURE_BLACK -> strings.dt("theme_amoled")
                            },
                            selected = theme.mode == mode,
                            onClick = { onThemeChange(theme.copy(mode = mode)) },
                        )
                    }
                }
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Edit,
                title = strings.dt("accent_color"),
                subtitle = AccentPalette[theme.accentIndex.coerceIn(0, AccentPalette.lastIndex)].label,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AccentPalette.forEachIndexed { index, option ->
                        val selected = theme.accentIndex == index
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(option.color, CircleShape)
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    shape = CircleShape,
                                )
                                .clickable { onThemeChange(theme.copy(accentIndex = index)) }
                        )
                    }
                }
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = strings.dt("visualizer"),
                subtitle = strings.dt("visualizer_desc"),
            ) {
                Switch(checked = visualizerEnabled, onCheckedChange = onVisualizerChange)
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Minimize,
                title = strings.dt("close_to_tray"),
                subtitle = strings.dt("close_to_tray_desc"),
            ) {
                Switch(checked = closeToTray, onCheckedChange = onCloseToTrayChange)
            }
        }

        SettingsGroup(strings.dt("playback_section")) {
            SettingsRow(
                icon = Icons.Rounded.GraphicEq,
                title = strings.dt("crossfade"),
                subtitle = strings.dt("crossfade_desc"),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0f, 2f, 4f, 6f).forEach { seconds ->
                        val label = if (seconds == 0f) {
                            strings.dt("gapless")
                        } else {
                            strings.dt("crossfade_value").format(seconds.toInt())
                        }
                        Chip(
                            text = label,
                            selected = crossfadeSeconds == seconds,
                            onClick = { onCrossfadeChange(seconds) },
                        )
                    }
                }
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.VolumeUp,
                title = strings.dt("replay_gain"),
                subtitle = strings.dt("replay_gain_desc"),
            ) {
                Switch(checked = replayGain, onCheckedChange = onReplayGainChange)
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = strings.get("equalizer"),
                subtitle = strings.getEQPresetDesc(eqPreset),
            ) {
                Text(
                    text = strings.getEQPresetLabel(eqPreset),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
            EQPreset.values().forEach { preset ->
                SettingsDivider()
                PresetRow(
                    strings = strings,
                    preset = preset,
                    selected = preset == eqPreset,
                    onClick = { onEqPresetChange(preset) },
                )
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Tune,
                title = strings.dt("volume"),
                subtitle = "${(volume * 100).toInt()}%",
            ) {
                Slider(
                    value = volume,
                    onValueChange = onVolumeChange,
                    modifier = Modifier.width(180.dp),
                )
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Nightlight,
                title = strings.get("sleep_timer"),
                subtitle = sleepRemaining?.let { formatLongTime(it / 1000.0) }
                    ?: if (sleepMinutes == 0) strings.dt("sleep_timer_off") else "$sleepMinutes ${strings.dt("min_short")}",
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 15, 30, 60, 90).forEach { minutes ->
                        Chip(
                            text = if (minutes == 0) strings.dt("sleep_timer_off") else "$minutes",
                            selected = sleepMinutes == minutes,
                            onClick = { onSleepMinutes(minutes) },
                        )
                    }
                }
            }
        }

        SettingsGroup(strings.get("library")) {
            SettingsRow(
                icon = Icons.Rounded.FolderOpen,
                title = strings.get("folders"),
                subtitle = folder?.absolutePath ?: strings.dt("no_folder"),
            ) {
                Chip(text = strings.get("folders"), selected = false, onClick = onChooseFolder)
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Refresh,
                title = strings.dt("refresh_library"),
                subtitle = strings.dt("refresh_library_desc"),
            ) {
                Chip(text = strings.get("refresh"), selected = false, onClick = onRefresh)
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.FolderOpen,
                title = strings.get("songs"),
                subtitle = "$trackCount ${strings.dt("tracks")} · ${formatLongTime(totalSeconds)}",
            ) {}
        }

        SettingsGroup(strings.get("language")) {
            SettingsRow(
                icon = Icons.Rounded.Language,
                title = strings.get("language"),
                subtitle = language.label,
            ) {}
            Spacer(Modifier.height(4.dp))
            AppLanguage.values().toList().chunked(4).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { option ->
                        Chip(
                            text = option.label,
                            selected = option == language,
                            onClick = { onLanguage(option) },
                        )
                    }
                }
            }
        }

        SettingsGroup(strings.get("about")) {
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = "BeatBox ${DesktopUpdateChecker.VERSION_NAME}",
                subtitle = strings.dt("desktop_build"),
            ) {}
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Refresh,
                title = strings.get("update_check"),
                subtitle = when (val state = updateState) {
                    UpdateUiState.Idle, UpdateUiState.Checking -> strings.get("update_check_desc")
                    UpdateUiState.UpToDate -> strings.get("update_up_to_date")
                    UpdateUiState.Failed -> strings.get("update_check_failed")
                    is UpdateUiState.Available ->
                        "${strings.get("update_version")}: ${state.update.versionName}"
                },
            ) {
                if (updateState is UpdateUiState.Available) {
                    Chip(
                        text = strings.get("update_download"),
                        selected = true,
                        onClick = onDownloadUpdate,
                    )
                } else {
                    val checking = updateState is UpdateUiState.Checking
                    Chip(
                        text = if (checking) "…" else strings.get("update_check"),
                        selected = false,
                        onClick = { if (!checking) onCheckUpdate() },
                    )
                }
            }
            val available = updateState as? UpdateUiState.Available
            if (available != null && available.update.changelog.isNotBlank()) {
                SettingsDivider()
                Text(
                    text = available.update.changelog,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 48.dp, end = 8.dp, top = 2.dp, bottom = 12.dp),
                )
            }
            SettingsDivider()
            SettingsRow(
                icon = Icons.Rounded.Info,
                title = strings.dt("supported_formats"),
                subtitle = "MP3, WAV, OGG, AIFF, AU",
            ) {}
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 48.dp)
            .height(1.dp)
            .background(Color.White.copy(alpha = GlassAlphaLow))
    )
}

@Composable
private fun PresetRow(
    strings: Strings,
    preset: EQPreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 9.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    width = 2.dp,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = strings.getEQPresetLabel(preset),
                fontSize = 13.5.sp,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
            Text(
                text = strings.getEQPresetDesc(preset),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val background = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(14.dp))
            .border(
                width = 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
