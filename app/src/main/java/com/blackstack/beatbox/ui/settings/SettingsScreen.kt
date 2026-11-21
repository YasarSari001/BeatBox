/*
 * BeatBox Project (C) 2026
 *
 * Ayarlar ekranı; Metrolist'in gruplanmış ayar düzeninden (GPL-3.0)
 * uyarlanmıştır. Kaynak: androidApp/src/main/kotlin/com/metrolist/android/
 * ui/screens/SettingsScreen.kt
 */

package com.blackstack.beatbox.ui.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenLockLandscape
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.blackstack.beatbox.ui.components.AlbumArtCache
import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.BuildConfig
import com.blackstack.beatbox.metadata.ArtistImageProvider
import com.blackstack.beatbox.MainActivity
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.components.UserAgreementDialog
import com.blackstack.beatbox.UserProfile
import com.blackstack.beatbox.ui.components.buttonShadow
import com.blackstack.beatbox.ui.components.cardShadow
import com.blackstack.beatbox.library.hasManageExternalStoragePermission
import com.blackstack.beatbox.ui.components.iconButtonShadow
import com.blackstack.beatbox.ui.theme.ThemeConfig

private enum class SettingsPage {
    MAIN,
    APPEARANCE,
    INTEGRATIONS,
}

/**
 * Ayarlar ekranı.
 *
 * Ana sayfa Metrolist'teki gibi gruplara ayrılmıştır. Görünüm grubuna
 * dokunulduğunda tema ayarlarının bulunduğu alt ekran açılır.
 */
@Composable
fun SettingsScreen(
    strings: Strings,
    userProfile: UserProfile,
    avatarIcons: List<ImageVector>,
    themeConfig: ThemeConfig,
    currentLang: AppLanguage,
    onThemeConfigChange: (ThemeConfig) -> Unit,
    onLangChange: (AppLanguage) -> Unit,
    tempoViz: Boolean,
    onTempoVizChange: (Boolean) -> Unit,
    prefs: SharedPreferences,
    ctx: Context,
    player: MainActivity,
    micLauncher: ActivityResultLauncher<String>,
    onProfileClick: () -> Unit,
    onOpenFolderDialog: () -> Unit,
    onOpenWrappedDialog: () -> Unit,
    onOpenHistoryDialog: () -> Unit,
    onClose: () -> Unit,
) {
    var page by remember { mutableStateOf(SettingsPage.MAIN) }

    BackHandler(enabled = page != SettingsPage.MAIN) { page = SettingsPage.MAIN }

    when (page) {
        SettingsPage.APPEARANCE -> AppearanceSettingsScreen(
            strings = strings,
            themeConfig = themeConfig,
            onThemeConfigChange = onThemeConfigChange,
            onBack = { page = SettingsPage.MAIN },
        )

        SettingsPage.INTEGRATIONS -> IntegrationsScreen(
            strings = strings,
            prefs = prefs,
            onBack = { page = SettingsPage.MAIN },
        )

        SettingsPage.MAIN -> MainSettingsPage(
            strings = strings,
            userProfile = userProfile,
            avatarIcons = avatarIcons,
            themeConfig = themeConfig,
            currentLang = currentLang,
            onLangChange = onLangChange,
            tempoViz = tempoViz,
            onTempoVizChange = onTempoVizChange,
            prefs = prefs,
            ctx = ctx,
            player = player,
            micLauncher = micLauncher,
            onProfileClick = onProfileClick,
            onOpenFolderDialog = onOpenFolderDialog,
            onOpenWrappedDialog = onOpenWrappedDialog,
            onOpenHistoryDialog = onOpenHistoryDialog,
            onOpenAppearance = { page = SettingsPage.APPEARANCE },
            onOpenIntegrations = { page = SettingsPage.INTEGRATIONS },
            onClose = onClose,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainSettingsPage(
    strings: Strings,
    userProfile: UserProfile,
    avatarIcons: List<ImageVector>,
    themeConfig: ThemeConfig,
    currentLang: AppLanguage,
    onLangChange: (AppLanguage) -> Unit,
    tempoViz: Boolean,
    onTempoVizChange: (Boolean) -> Unit,
    prefs: SharedPreferences,
    ctx: Context,
    player: MainActivity,
    micLauncher: ActivityResultLauncher<String>,
    onProfileClick: () -> Unit,
    onOpenFolderDialog: () -> Unit,
    onOpenWrappedDialog: () -> Unit,
    onOpenHistoryDialog: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenIntegrations: () -> Unit,
    onClose: () -> Unit,
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var storageAccessGranted by remember { mutableStateOf(hasManageExternalStoragePermission(ctx)) }
    val storageAccessLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        storageAccessGranted = hasManageExternalStoragePermission(ctx)
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.statusBarsPadding()) {
            TopAppBar(
                title = { Text(strings.get("settings"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onClose, modifier = Modifier.iconButtonShadow()) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.get("back"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(Color.Transparent),
            )
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            ) {
                item { ProfileCard(userProfile, avatarIcons, strings, onProfileClick) }
                item {
                    SettingsGroupCard(title = strings.get("appearance")) {
                        SettingsActionRow(
                            icon = Icons.Rounded.Palette,
                            title = strings.get("appearance_title"),
                            subtitle = themeModeLabel(strings, themeConfig.mode),
                            showChevron = true,
                            onClick = onOpenAppearance,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Rounded.Language,
                            title = strings.get("language"),
                            value = currentLang.label,
                            showChevron = true,
                            onClick = { showLanguageDialog = true },
                        )
                    }
                }
                item {
                    SettingsGroupCard(title = strings.get("library")) {
                        SettingsActionRow(
                            icon = Icons.Rounded.Folder,
                            title = strings.get("folders"),
                            showChevron = true,
                            onClick = onOpenFolderDialog,
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            SettingsDivider()
                            SettingsActionRow(
                                icon = Icons.Rounded.Storage,
                                title = strings.get("storage_access"),
                                subtitle = if (storageAccessGranted) {
                                    strings.get("storage_access_granted")
                                } else {
                                    strings.get("storage_access_desc")
                                },
                                showChevron = true,
                                onClick = {
                                    val appSettings = Intent(
                                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                    ).setData(Uri.parse("package:${ctx.packageName}"))
                                    runCatching { storageAccessLauncher.launch(appSettings) }
                                        .onFailure {
                                            runCatching {
                                                storageAccessLauncher.launch(
                                                    Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                )
                                            }
                                        }
                                },
                            )
                        }
                        SettingsDivider()
                        var autoMetadata by remember {
                            mutableStateOf(prefs.getBoolean("auto_song_metadata", true))
                        }
                        SettingsToggleRow(
                            icon = Icons.Rounded.AutoAwesome,
                            title = strings.get("auto_song_metadata"),
                            checked = autoMetadata,
                            onCheckedChange = { enabled ->
                                autoMetadata = enabled
                                prefs.edit { putBoolean("auto_song_metadata", enabled) }
                            },
                        )
                    }
                }
                item {
                    SettingsGroupCard(title = strings.get("playback")) {
                        SettingsToggleRow(
                            icon = Icons.Rounded.Tune,
                            title = strings.get("tempo_visualization"),
                            checked = tempoViz,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    val needsMic = ContextCompat.checkSelfPermission(
                                        ctx, Manifest.permission.RECORD_AUDIO
                                    ) != PackageManager.PERMISSION_GRANTED
                                    if (needsMic) {
                                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        onTempoVizChange(true)
                                        prefs.edit { putBoolean("tempo_visualization", true) }
                                    }
                                } else {
                                    onTempoVizChange(false)
                                    prefs.edit { putBoolean("tempo_visualization", false) }
                                }
                            },
                        )
                        SettingsDivider()
                        var performanceMode by remember {
                            mutableStateOf(prefs.getBoolean("performance_mode", false))
                        }
                        SettingsToggleRow(
                            icon = Icons.Rounded.Speed,
                            title = strings.get("performance_mode"),
                            checked = performanceMode,
                            onCheckedChange = { enabled ->
                                performanceMode = enabled
                                prefs.edit { putBoolean("performance_mode", enabled) }
                                if (enabled) {
                                    onTempoVizChange(false)
                                    prefs.edit { putBoolean("tempo_visualization", false) }
                                }
                            },
                        )
                        SettingsDivider()
                        var shakeToShuffle by remember {
                            mutableStateOf(prefs.getBoolean("shake_to_shuffle", true))
                        }
                        SettingsToggleRow(
                            icon = Icons.Rounded.Shuffle,
                            title = strings.get("shake_to_shuffle"),
                            checked = shakeToShuffle,
                            onCheckedChange = {
                                shakeToShuffle = it
                                prefs.edit { putBoolean("shake_to_shuffle", it) }
                            },
                        )
                        SettingsDivider()
                        var crossfade by remember {
                            mutableStateOf(prefs.getBoolean("crossfade_enabled", true))
                        }
                        SettingsToggleRow(
                            icon = Icons.Rounded.SwapHoriz,
                            title = strings.get("crossfade"),
                            checked = crossfade,
                            onCheckedChange = {
                                crossfade = it
                                prefs.edit { putBoolean("crossfade_enabled", it) }
                            },
                        )
                        SettingsDivider()
                        var keepScreenOn by remember {
                            mutableStateOf(prefs.getBoolean("keep_screen_on", false))
                        }
                        SettingsToggleRow(
                            icon = Icons.Rounded.ScreenLockLandscape,
                            title = strings.get("keep_screen_on"),
                            checked = keepScreenOn,
                            onCheckedChange = { enabled ->
                                keepScreenOn = enabled
                                player.setKeepScreenOn(enabled)
                            },
                        )
                    }
                }
                item {
                    SettingsGroupCard(title = strings.get("personalization")) {
                        SettingsActionRow(
                            icon = Icons.Rounded.Analytics,
                            title = strings.get("wrapped_title"),
                            subtitle = strings.get("wrapped_desc"),
                            showChevron = true,
                            onClick = onOpenWrappedDialog,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Rounded.History,
                            title = strings.get("listening_history"),
                            subtitle = strings.get("history_desc"),
                            showChevron = true,
                            onClick = onOpenHistoryDialog,
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Rounded.CleaningServices,
                            title = strings.get("clear_cache"),
                            subtitle = strings.get("cache_subtitle"),
                            onClick = {
                                ArtistImageProvider.bitmapCache.evictAll()
                                AlbumArtCache.bitmapCache.evictAll()
                                Toast.makeText(ctx, strings.get("cache_cleared"), Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
                item {
                    SettingsGroupCard(title = strings.get("integrations")) {
                        SettingsActionRow(
                            icon = Icons.Rounded.Extension,
                            title = strings.get("integrations"),
                            subtitle = strings.get("integrations_desc"),
                            showChevron = true,
                            onClick = onOpenIntegrations,
                        )
                    }
                }
                item {
                    SettingsGroupCard(title = strings.get("about")) {
                        SettingsActionRow(
                            icon = Icons.Rounded.Info,
                            title = strings.get("about"),
                            value = "BeatBox ${BuildConfig.VERSION_NAME}",
                            showChevron = true,
                            onClick = { showAboutDialog = true },
                        )
                        SettingsDivider()
                        SettingsActionRow(
                            icon = Icons.Rounded.Refresh,
                            title = strings.get("update_check"),
                            subtitle = strings.get("update_check_desc"),
                            showChevron = true,
                            onClick = { player.checkForUpdatesManually() },
                        )
                    }
                }
            }
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            strings = strings,
            currentLang = currentLang,
            onLangChange = onLangChange,
            onDismiss = { showLanguageDialog = false },
        )
    }

    if (showAboutDialog) {
        AboutDialog(strings = strings, onDismiss = { showAboutDialog = false })
    }
}

@Composable
private fun ProfileCard(
    userProfile: UserProfile,
    avatarIcons: List<ImageVector>,
    strings: Strings,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
            .cardShadow(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = avatarIcons.getOrElse(userProfile.avatarId) { Icons.Rounded.Person },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = userProfile.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = if (userProfile.isLoggedIn) {
                        userProfile.email.ifBlank { userProfile.bio }
                    } else {
                        strings.get("login_prompt")
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = if (userProfile.isLoggedIn) strings.get("edit") else strings.get("login"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LanguageDialog(
    strings: Strings,
    currentLang: AppLanguage,
    onLangChange: (AppLanguage) -> Unit,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val systemLang = remember(configuration) {
        when (configuration.locales[0].language.lowercase()) {
            "tr" -> AppLanguage.TR
            "en" -> AppLanguage.EN
            "es" -> AppLanguage.ES
            "fr" -> AppLanguage.FR
            "de" -> AppLanguage.DE
            "it" -> AppLanguage.IT
            "pt" -> AppLanguage.PT
            "ru" -> AppLanguage.RU
            "ja" -> AppLanguage.JA
            "zh" -> AppLanguage.ZH
            "ar" -> AppLanguage.AR
            "ko" -> AppLanguage.KO
            else -> AppLanguage.EN
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.buttonShadow(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(strings.get("ok"))
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
                Spacer(Modifier.width(12.dp))
                Text(strings.get("language"), fontWeight = FontWeight.Bold)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AppLanguage.entries) { language ->
                    val selected = currentLang == language
                    val systemDefault = language == systemLang
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onLangChange(language)
                                onDismiss()
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        },
                        border = if (selected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            null
                        },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        } else {
                                            Color.Transparent
                                        },
                                        CircleShape,
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = language.code.uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = language.label,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (systemDefault) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.PhoneAndroid,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            modifier = Modifier.size(13.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = strings.get("system_default"),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
    )
}

@Composable
private fun AboutDialog(
    strings: Strings,
    onDismiss: () -> Unit,
) {
    var showUserAgreement by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss, modifier = Modifier.buttonShadow()) {
                Text(strings.get("ok"))
            }
        },
        title = { Text(strings.get("about")) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = strings.get("about_desc"),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(12.dp))
                TextButton(
                    onClick = { showUserAgreement = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = strings.get("user_agreement"),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
    )

    if (showUserAgreement) {
        UserAgreementDialog(
            strings = strings,
            onDismiss = { showUserAgreement = false },
        )
    }
}
