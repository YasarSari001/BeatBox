package com.blackstack.beatbox.ui.components
import androidx.core.net.toUri
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.os.Build
import android.provider.MediaStore
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.palette.graphics.Palette
import com.blackstack.beatbox.ui.theme.BeatBoxTheme
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.AuthProvider
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.UserProfile

// ═══════════════════════════════════════════
// UI COMPONENTS
// ═══════════════════════════════════════════

// Unified cache and concurrency control for album art loading (prevents ANR and memory leaks)
internal object AlbumArtCache {
    // Unified cache shared with MediaPlaybackService to prevent duplicate bitmap storage and OOM
    val bitmapCache = com.blackstack.beatbox.playback.bitmapCache
}


// Explicit Content Badge
@Composable
fun ExplicitBadge(
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.88f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = "E",
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onErrorContainer
            ),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

// Shadow effects for buttons
fun Modifier.buttonShadow() = this.shadow(
    elevation = 4.dp,
    shape = RoundedCornerShape(12.dp),
    ambientColor = Color.Black.copy(alpha = 0.1f),
    spotColor = Color.Black.copy(alpha = 0.1f)
)

fun Modifier.iconButtonShadow() = this.shadow(
    elevation = 2.dp,
    shape = CircleShape,
    ambientColor = Color.Black.copy(alpha = 0.08f),
    spotColor = Color.Black.copy(alpha = 0.08f)
)

fun Modifier.cardShadow() = this.shadow(
    elevation = 6.dp,
    shape = RoundedCornerShape(28.dp),
    ambientColor = Color.Black.copy(alpha = 0.12f),
    spotColor = Color.Black.copy(alpha = 0.12f)
)

fun Modifier.fabShadow() = this.shadow(
    elevation = 8.dp,
    shape = RoundedCornerShape(28.dp),
    ambientColor = Color.Black.copy(alpha = 0.15f),
    spotColor = Color.Black.copy(alpha = 0.15f)
)



fun formatTime(ms: Long): String = String.format(Locale.getDefault(), "%02d:%02d", TimeUnit.MILLISECONDS.toMinutes(ms), TimeUnit.MILLISECONDS.toSeconds(ms) % 60)

fun loadUserProfile(prefs: SharedPreferences, strings: Strings): UserProfile {
    val isLoggedIn = prefs.getBoolean("user_is_logged_in", false)
    if (!isLoggedIn) {
        return UserProfile(isLoggedIn = false)
    }
    return UserProfile(
        id = prefs.getString("user_id", "user_1") ?: "user_1",
        displayName = prefs.getString("user_display_name", strings.get("listener"))
            ?: strings.get("listener"),
        email = prefs.getString("user_email", "") ?: "",
        phoneNumber = prefs.getString("user_phone", "") ?: "",
        bio = prefs.getString("user_bio", strings.get("default_bio"))
            ?: strings.get("default_bio"),
        avatarId = prefs.getInt("user_avatar_id", 0),
        joinedDate = prefs.getLong("user_joined_date", System.currentTimeMillis()),
        isLoggedIn = true,
        authProvider = try {
            AuthProvider.valueOf(prefs.getString("user_auth_provider", "EMAIL") ?: "EMAIL")
        } catch (e: Exception) { AuthProvider.EMAIL },
        isPremium = prefs.getBoolean("user_is_premium", true)
    )
}

fun saveUserProfile(prefs: SharedPreferences, user: UserProfile) {
    prefs.edit()
        .putBoolean("user_is_logged_in", user.isLoggedIn)
        .putString("user_id", user.id)
        .putString("user_display_name", user.displayName)
        .putString("user_email", user.email)
        .putString("user_phone", user.phoneNumber)
        .putString("user_bio", user.bio)
        .putInt("user_avatar_id", user.avatarId)
        .putLong("user_joined_date", user.joinedDate)
        .putString("user_auth_provider", user.authProvider.name)
        .putBoolean("user_is_premium", user.isPremium)
        .apply()
}

fun updateSongStats(prefs: SharedPreferences, a: AudioFile) {
    val recentHistory = prefs.getString("stats_history", "") ?: ""
    val historyList = recentHistory.split(",").filter { it.isNotBlank() }.toMutableList()
    historyList.remove(a.id.toString())
    historyList.add(0, a.id.toString())
    val newHistory = historyList.take(50).joinToString(",")

    prefs.edit()
        .putInt("stats_count_${a.id}", prefs.getInt("stats_count_${a.id}", 0) + 1)
        .putLong("stats_last_${a.id}", System.currentTimeMillis())
        .putString("stats_history", newHistory)
        .apply()
}

fun loadAudioFilesAndFolders(ctx: Context, minSec: Int, selected: Set<String>): Pair<List<AudioFile>, List<String>> {
    val list = mutableListOf<AudioFile>()
    val folders = mutableSetOf<String>()
    
    try {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.ALBUM
        )
        
        // Add duration filter to SQL query to reduce processing
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf((minSec * 1000).toString())
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"
        
        ctx.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val count = cursor.count
            android.util.Log.d("AudioLoader", "Loading $count audio files")
            
            while (cursor.moveToNext()) {
                try {
                    val path = cursor.getString(5) ?: continue
                    val folderName = File(path).parentFile?.name ?: "Unknown"
                    folders.add(folderName)
                    
                    // Skip if folder filtering is active and this folder is not selected
                    if (selected.isNotEmpty() && !selected.contains(folderName)) {
                        continue
                    }
                    
                    val id = cursor.getLong(0)
                    val audioUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    val audioFile = AudioFile(
                        id = id,
                        title = cursor.getString(1) ?: "Unknown",
                        artist = cursor.getString(2) ?: "Unknown",
                        duration = cursor.getLong(3),
                        uri = audioUri,
                        albumId = cursor.getLong(4),
                        folderName = folderName,
                        dateAdded = cursor.getLong(6),
                        albumName = cursor.getString(7)
                            ?.trim()
                            ?.takeIf { it.isNotBlank() && !it.equals("<unknown>", true) }
                    )

                    // Dosya adı düzeltmesi burada yapılmaz; tarama hızlı kalır.
                    // Etiketler gösterimden sonra arka planda ayrı bir geçişte
                    // yazılır (bkz. LibraryState.startMetadataFix).
                    list.add(audioFile)
                } catch (e: Exception) {
                    // Log individual file errors but continue processing
                    android.util.Log.w("AudioLoader", "Error processing audio file", e)
                }
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("AudioLoader", "Error loading audio files", e)
        // Return empty lists on critical error
        return Pair(emptyList(), emptyList())
    }
    
    android.util.Log.d("AudioLoader", "Loaded ${list.size} audio files from ${folders.size} folders")
    return Pair(list, folders.sorted())
}

/**
 * Coarse classification of an audio output device.
 *
 * Kept separate from [getAudioOutputDeviceName] so that the mapping can be
 * verified in a plain JVM unit test without an Android Context.
 */
enum class AudioOutputKind { BLUETOOTH, HEADPHONES, NONE }

/**
 * Maps an [AudioDeviceInfo] type constant to an [AudioOutputKind].
 *
 * Returns [AudioOutputKind.NONE] for the built-in speaker and for every
 * device type the app does not want to advertise (for example HDMI or USB
 * accessories that the user has not selected).
 */
fun audioOutputKindFor(deviceType: Int): AudioOutputKind = when (deviceType) {
    AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
    AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
    AudioDeviceInfo.TYPE_BLE_HEADSET,
    AudioDeviceInfo.TYPE_BLE_SPEAKER -> AudioOutputKind.BLUETOOTH

    AudioDeviceInfo.TYPE_WIRED_HEADSET,
    AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
    AudioDeviceInfo.TYPE_USB_HEADSET,
    AudioDeviceInfo.TYPE_USB_DEVICE -> AudioOutputKind.HEADPHONES

    else -> AudioOutputKind.NONE
}

fun getAudioOutputDeviceName(context: Context, strings: Strings): String {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        ?: return strings.get("phone_speaker")

    val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    for (device in devices) {
        when (audioOutputKindFor(device.type)) {
            AudioOutputKind.BLUETOOTH -> return strings.get("bluetooth")
            AudioOutputKind.HEADPHONES -> return strings.get("headphones")
            AudioOutputKind.NONE -> {}
        }
    }

    @Suppress("DEPRECATION")
    if (audioManager.isBluetoothA2dpOn || audioManager.isBluetoothScoOn) {
        return strings.get("bluetooth")
    }

    @Suppress("DEPRECATION")
    if (audioManager.isWiredHeadsetOn) {
        return strings.get("headphones")
    }

    return strings.get("phone_speaker")
}

@Composable
fun AudioOutputIndicator(
    strings: Strings,
    context: Context = LocalContext.current,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var isVisible by remember { mutableStateOf(false) }
    var currentDevice by remember(strings.lang) { mutableStateOf(strings.get("phone_speaker")) }

    LaunchedEffect(isPlaying, strings.lang) {
        if (isPlaying) {
            currentDevice = getAudioOutputDeviceName(context, strings)
            isVisible = true
            delay(3000)
            isVisible = false
        } else {
            isVisible = false
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(500)),
        modifier = modifier
    ) {
        val icon = when (currentDevice) {
            strings.get("bluetooth") -> Icons.Rounded.Bluetooth
            strings.get("headphones") -> Icons.Rounded.Headphones
            else -> Icons.Rounded.PhoneAndroid
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = currentDevice,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = currentDevice,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
