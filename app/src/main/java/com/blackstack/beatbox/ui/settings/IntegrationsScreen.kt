/*
 * BeatBox Project (C) 2026
 *
 * Metrolist'in entegrasyon ayarları ekranından (GPL-3.0) uyarlanmıştır.
 * Kaynak: app/src/main/kotlin/com/metrolist/music/ui/screens/settings/integrations/
 */

package com.blackstack.beatbox.ui.settings

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.integrations.ListenBrainzClient
import com.blackstack.beatbox.integrations.ListenBrainzSettings
import com.blackstack.beatbox.lyrics.LyricsPreferences
import com.blackstack.beatbox.ui.components.iconButtonShadow
import kotlinx.coroutines.launch

/**
 * Entegrasyonlar sayfası.
 *
 * ListenBrainz scrobbling'i ve şarkı sözü sağlayıcı zincirini yönetir.
 * Discord/Last.fm gibi uygulama kimliği veya geliştirici anahtarı gerektiren
 * servisler bilinçli olarak yok; yalnızca burada yapılandırılabilen ve gerçekten
 * çalışan entegrasyonlar gösterilir.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntegrationsScreen(
    strings: Strings,
    prefs: SharedPreferences,
    onBack: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var token by remember { mutableStateOf(stringsToken(prefs)) }
    var enabled by remember { mutableStateOf(ListenBrainzSettings.load(prefs).enabled) }
    var nowPlaying by remember { mutableStateOf(ListenBrainzSettings.load(prefs).nowPlaying) }
    var providers by remember { mutableStateOf(LyricsPreferences.load(prefs).enabledProviders) }
    var showTokenDialog by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<String?>(null) }
    var testSucceeded by remember { mutableStateOf(false) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        TopAppBar(
            title = { Text(strings.get("integrations"), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack, modifier = Modifier.iconButtonShadow()) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.get("back"),
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(androidx.compose.ui.graphics.Color.Transparent),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        ) {
            item {
                SettingsGroupCard(title = strings.get("listenbrainz")) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.CloudUpload,
                        title = strings.get("listenbrainz_enabled"),
                        subtitle = strings.get("listenbrainz_desc"),
                        checked = enabled,
                        onCheckedChange = { value ->
                            enabled = value
                            ListenBrainzSettings.saveEnabled(prefs, value)
                            testResult = null
                        },
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = Icons.Rounded.Key,
                        title = strings.get("listenbrainz_token"),
                        subtitle = if (token.isBlank()) {
                            strings.get("listenbrainz_token_missing")
                        } else {
                            "•••• " + token.takeLast(4)
                        },
                        value = if (token.isBlank()) strings.get("set") else strings.get("edit"),
                        showChevron = true,
                        onClick = { showTokenDialog = true },
                    )
                    SettingsDivider()
                    SettingsToggleRow(
                        icon = Icons.Rounded.Headphones,
                        title = strings.get("now_playing"),
                        checked = nowPlaying,
                        enabled = enabled,
                        onCheckedChange = { value ->
                            nowPlaying = value
                            ListenBrainzSettings.saveNowPlaying(prefs, value)
                        },
                    )
                    SettingsDivider()
                    SettingsActionRow(
                        icon = if (testSucceeded) Icons.Rounded.CheckCircle else Icons.Rounded.VerifiedUser,
                        title = strings.get("test_connection"),
                        subtitle = testResult,
                        enabled = !testing && token.isNotBlank(),
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                ListenBrainzClient.validateToken(token)
                                    .onSuccess { name ->
                                        testSucceeded = true
                                        testResult = strings.get("connected_as").replace("%s", name)
                                    }
                                    .onFailure { error ->
                                        testSucceeded = false
                                        testResult = strings.get("connection_failed")
                                            .replace("%s", error.message ?: "?")
                                    }
                                testing = false
                            }
                        },
                    )
                    if (testing) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(start = 66.dp, bottom = 12.dp)
                                .height(18.dp)
                        )
                    }
                }
            }
            item {
                SettingsGroupCard(title = strings.get("lyrics_providers")) {
                    Text(
                        text = strings.get("lyrics_providers_desc"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                    LyricsPreferences.PROVIDERS.forEachIndexed { index, provider ->
                        if (index > 0) SettingsDivider()
                        SettingsToggleRow(
                            icon = Icons.Rounded.Lyrics,
                            title = provider,
                            checked = provider in providers,
                            onCheckedChange = { checked ->
                                val updated = if (checked) providers + provider else providers - provider
                                // En az bir sağlayıcı açık kalmalı; aksi halde
                                // söz araması hiçbir zaman çalışmaz.
                                if (updated.isNotEmpty()) {
                                    providers = updated
                                    LyricsPreferences.saveProviders(prefs, updated)
                                }
                            },
                        )
                    }
                }
            }
            item {
                SettingsGroupCard(title = strings.get("lyrics_settings")) {
                    var size by remember {
                        mutableStateOf(LyricsPreferences.load(prefs).textSizeSp)
                    }
                    Text(
                        text = strings.get("lyrics_size_hint")
                            .replace("%s", size.toInt().toString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                    androidx.compose.material3.Slider(
                        value = size,
                        onValueChange = { size = it },
                        onValueChangeFinished = {
                            LyricsPreferences.saveTextSize(prefs, size)
                        },
                        valueRange = LyricsPreferences.MIN_TEXT_SIZE..LyricsPreferences.MAX_TEXT_SIZE,
                        steps = 8,
                        modifier = Modifier.padding(horizontal = 18.dp),
                    )
                    SettingsDivider()
                    var glow by remember { mutableStateOf(LyricsPreferences.load(prefs).glowEnabled) }
                    SettingsToggleRow(
                        icon = Icons.Rounded.Lyrics,
                        title = strings.get("lyrics_glow"),
                        checked = glow,
                        onCheckedChange = {
                            glow = it
                            LyricsPreferences.saveGlow(prefs, it)
                        },
                    )
                    SettingsDivider()
                    var clickSeek by remember { mutableStateOf(LyricsPreferences.load(prefs).clickToSeek) }
                    SettingsToggleRow(
                        icon = Icons.Rounded.Lyrics,
                        title = strings.get("lyrics_click_seek"),
                        checked = clickSeek,
                        onCheckedChange = {
                            clickSeek = it
                            LyricsPreferences.saveClickToSeek(prefs, it)
                        },
                    )
                }
            }
        }
    }
    }

    if (showTokenDialog) {
        var draft by remember { mutableStateOf(token) }
        AlertDialog(
            onDismissRequest = { showTokenDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        token = draft.trim()
                        ListenBrainzSettings.saveToken(prefs, token)
                        ListenBrainzSettings.saveEnabled(prefs, enabled)
                        showTokenDialog = false
                        testResult = null
                    },
                ) { Text(strings.get("save")) }
            },
            dismissButton = {
                TextButton(onClick = { showTokenDialog = false }) { Text(strings.get("cancel")) }
            },
            title = { Text(strings.get("listenbrainz_token"), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        label = { Text(strings.get("token")) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = strings.get("listenbrainz_token_hint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    testResult?.let { message ->
                        Spacer(Modifier.height(10.dp))
                        Icon(
                            imageVector = if (testSucceeded) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            tint = if (testSucceeded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        )
                        Text(message, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            shape = RoundedCornerShape(28.dp),
        )
    }
}

private fun stringsToken(prefs: SharedPreferences): String =
    prefs.getString("listenbrainz_token", "").orEmpty()
