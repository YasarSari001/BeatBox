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
import kotlinx.coroutines.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit
import com.blackstack.beatbox.metadata.ArtworkDecoder
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.metadata.SongMetadataRepository

@Composable fun AlbumArtImage(a: AudioFile, modifier: Modifier = Modifier, isHQ: Boolean = false) {
    val ctx = LocalContext.current
    val resolvedPath = SongMetadataRepository.resolvedArtwork[a.id]
    val targetSize = if (isHQ) 256 else 96
    var bmp by remember(a.id, a.artworkPath, resolvedPath) {
        mutableStateOf<Bitmap?>(AlbumArtCache.bitmapCache.get(a.id))
    }
    var isLoading by remember(a.id, a.artworkPath, resolvedPath) { mutableStateOf(false) }
    
    LaunchedEffect(a.id, a.artworkPath, resolvedPath) {
        if (bmp == null) {
            // Check memory cache first
            AlbumArtCache.bitmapCache.get(a.id)?.let {
                bmp = it
                return@LaunchedEffect
            }
            
            isLoading = true
            withContext(Dispatchers.IO) {
                var bitmap: Bitmap? = null

                // 1. Fast system MediaStore album art URI FIRST (2ms execution!)
                if (a.albumId > 0) {
                    try {
                        val uri = ContentUris.withAppendedId(
                            "content://media/external/audio/albumart".toUri(), 
                            a.albumId
                        )
                        bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ctx.contentResolver.loadThumbnail(
                                uri,
                                android.util.Size(targetSize, targetSize),
                                null
                            )
                        } else {
                            ArtworkDecoder.decode(ctx, uri, targetSize)
                        }
                    } catch (_: Exception) {}
                }

                // 2. Secondary Fallback: Embedded album art via file descriptor (only if MediaStore URI is missing)
                if (bitmap == null) {
                    try {
                        ctx.contentResolver.openFileDescriptor(a.uri, "r")?.use { pfd ->
                            val retriever = MediaMetadataRetriever()
                            try {
                                retriever.setDataSource(pfd.fileDescriptor)
                                val art = retriever.embeddedPicture
                                if (art != null) {
                                    bitmap = ArtworkDecoder.decode(art, targetSize)
                                }
                            } finally {
                                try {
                                    retriever.release()
                                } catch (_: Exception) {
                                }
                            }
                        }
                    } catch (_: Exception) {}
                }

                // 3. Önceden indirilmiş çevrim içi kapak.
                if (bitmap == null) {
                    val cachedPath = a.artworkPath ?: resolvedPath
                    if (!cachedPath.isNullOrBlank()) {
                        bitmap = decodeCachedArtwork(cachedPath, isHQ)
                    }
                }

                // 4. Yerel kapak yoksa çevrim içi kaynaklardan tamamla.
                //    Eşleşme bulunamazsa mevcut gradyan yer tutucu kalır.
                if (bitmap == null) {
                    val fetchedPath = SongMetadataRepository.ensureArtwork(ctx, a)
                    if (fetchedPath != null) {
                        bitmap = decodeCachedArtwork(fetchedPath, isHQ)
                    }
                }

                if (bitmap != null) {
                    AlbumArtCache.bitmapCache.put(a.id, bitmap)
                }
                
                withContext(Dispatchers.Main) {
                    bmp = bitmap
                    isLoading = false
                }
            }
        }
    }
    
    bmp?.let { validBitmap ->
        Box(modifier) {
            Image(
                bitmap = validBitmap.asImageBitmap(), 
                contentDescription = null, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize()
            )
        }
    } ?: run {
        // Use cached gradient background when no album art is available
        val gradient = remember(a.id) { gradientForSong(a, ctx) }
        Box(
            modifier = modifier.background(gradient), 
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = Color.Gray.copy(alpha = 0.5f))
            }
        }
    }
}

/** Disk önbelleğindeki kapağı liste boyutuna uygun şekilde çözer. */
private fun decodeCachedArtwork(path: String, isHQ: Boolean): Bitmap? =
    ArtworkDecoder.decode(File(path), if (isHQ) 256 else 96)

// Generates a pleasant gradient based on song title/artist to use when album art is missing
private fun colorFromString(seed: String): Color {
    val h = seed.hashCode()
    val r = ((h shr 16) and 0xFF)
    val g = ((h shr 8) and 0xFF)
    val b = (h and 0xFF)
    val a = 0xFF
    return Color((a shl 24) or (r shl 16) or (g shl 8) or b)
}

// LRU Cache for gradient calculations to improve performance and prevent memory issues
private val gradientCache = object : LinkedHashMap<String, Brush>(
    100, // Initial capacity
    0.75f, // Load factor
    true // Access order (LRU)
) {
    override fun removeEldestEntry(eldest: Map.Entry<String, Brush>): Boolean {
        return size > 100 // Remove oldest entry when size exceeds 100
    }
}

private fun gradientForSong(a: AudioFile, ctx: Context): Brush {
    val cacheKey = "${a.id}_${a.title}_${a.artist}"
    
    // Return cached gradient if available
    gradientCache[cacheKey]?.let { return it }
    
    // Simple gradient based on song info (no user preference)
    val c1 = colorFromString(a.title + a.artist)
    val c2 = colorFromString(a.title + a.artist + "grad2")
    val gradient = Brush.linearGradient(listOf(c1, c2))
    
    // Cache the gradient (LRU will automatically remove oldest entry if needed)
    gradientCache[cacheKey] = gradient
    
    return gradient
}
