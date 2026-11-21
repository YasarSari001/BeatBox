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
import com.blackstack.beatbox.Strings

@Composable fun EmptyState(icon: ImageVector, t: String) {
    Box(Modifier.fillMaxSize(), Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(icon, null, Modifier.size(64.dp), Color.Gray); Spacer(Modifier.height(16.dp)); Text(t, color = Color.Gray) } }
}

@Composable fun PermissionScreen(strings: Strings, onClick: () -> Unit) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.MusicNote, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp)); Text(strings.get("permission_required")); Spacer(Modifier.height(16.dp))
            Button(onClick, modifier = Modifier.buttonShadow()) { Text(strings.get("grant_permission")) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable fun SongItem(
    a: AudioFile,
    strings: Strings,
    isCurrent: Boolean,
    isPlaying: Boolean = false,
    isFavorite: Boolean,
    onFavClick: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onShowArtistSongs: () -> Unit = {},
    onDownload: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Modern Glassmorphic card design
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
            .border(
                width = if (isCurrent) 1.5.dp else 1.dp,
                brush = Brush.linearGradient(
                    colors = if (isCurrent) {
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(0.3f)
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.onSurface.copy(0.12f),
                            MaterialTheme.colorScheme.onSurface.copy(0.02f)
                        )
                    }
                ),
                shape = RoundedCornerShape(22.dp)
            )
            .shadow(
                elevation = if (isCurrent) 10.dp else 4.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.08f),
                spotColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(0.3f) else Color.Black.copy(0.08f)
            ),
        shape = RoundedCornerShape(22.dp),
        color = if (isCurrent) 
            MaterialTheme.colorScheme.primaryContainer.copy(0.9f) 
        else 
            MaterialTheme.colorScheme.surfaceVariant.copy(0.88f)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art with glow effect when playing
            Box(contentAlignment = Alignment.Center) {
                Card(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(if (isCurrent) 6.dp else 2.dp)
                ) {
                    AlbumArtImage(a, Modifier.fillMaxSize(), isHQ = true)
                }
                
                // Playing indicator overlay / Equalizer animation
                if (isCurrent && isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedEqualizer(isPlaying = true, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            // Song info
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        a.title,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCurrent) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (a.isExplicit) {
                        Spacer(Modifier.width(6.dp))
                        ExplicitBadge()
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    a.artist,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatDuration(a.duration),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.75f),
                    fontWeight = FontWeight.Normal
                )
            }
            
            // Favorite button with animation
            IconButton(
                onClick = onFavClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isFavorite) 
                            Color(0xFFFF4081).copy(0.15f) 
                        else 
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                        CircleShape
                    )
            ) {
                Icon(
                    if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    null,
                    tint = if (isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(Modifier.width(4.dp))
            
            // Menu button
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.08f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(showMenu, { showMenu = false }) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get("same_artist_songs"),
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onShowArtistSongs(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Person,
                                null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get(if (isFavorite) "remove_from_favorites" else "add_to_favorites"),
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onFavClick(); showMenu = false },
                        leadingIcon = {
                            Icon(
                                if (isFavorite) Icons.Rounded.FavoriteBorder else Icons.Rounded.Favorite,
                                null,
                                tint = if (!isFavorite) Color(0xFFFF4081) else MaterialTheme.colorScheme.onSurface.copy(0.6f)
                            )
                        }
                    )
                    if (a.isOnline) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    strings.get("download"),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            onClick = { onDownload(); showMenu = false },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Download,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { 
                            Text(
                                strings.get("delete"),
                                color = Color.Red,
                                fontWeight = FontWeight.Medium
                            ) 
                        },
                        onClick = { onDelete(); showMenu = false },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                    )
                }
            }
        }
    }
}

// Helper function to format duration
private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000).toInt()
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(java.util.Locale.getDefault(), "%d:%02d", minutes, secs)
}
