package com.blackstack.beatbox.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jaudiotagger.audio.AudioFileIO
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Gömülü albüm kapağı okuyucu.
 *
 * Kapak JPEG/PNG olarak etikette saklanır; çözülen görüntü dosya yoluyla
 * önbelleğe alınır, böylece ızgara kaydırılırken tekrar tekrar disk okunmaz.
 */
object AlbumArt {
    private val cache = ConcurrentHashMap<String, ImageBitmap?>()

    fun cover(track: Track): ImageBitmap? {
        val key = track.file.absolutePath
        if (cache.containsKey(key)) return cache[key]
        val bytes = runCatching {
            AudioFileIO.read(track.file).tag?.firstArtwork?.binaryData
        }.getOrNull()
        val bitmap = bytes?.let(::decode)
        cache[key] = bitmap
        return bitmap
    }

    private fun decode(bytes: ByteArray): ImageBitmap? = runCatching {
        ImageIO.read(ByteArrayInputStream(bytes))?.toComposeImageBitmap()
    }.getOrNull()
}

/** Albüm kartlarında gösterilen gruptur. */
data class AlbumGroup(
    val key: String,
    val title: String,
    val artist: String,
    val tracks: List<Track>,
)
