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
    // ConcurrentHashMap null değer kabul etmediği için kapağı olmayan
    // dosyalar ayrı bir kümede işaretlenir; böylece disk tekrar okunmaz.
    private val cache = ConcurrentHashMap<String, ImageBitmap>()
    private val missing = java.util.Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())

    fun cover(track: Track): ImageBitmap? {
        val key = track.file.absolutePath
        cache[key]?.let { return it }
        if (key in missing) return null
        val bytes = runCatching {
            AudioFileIO.read(track.file).tag?.firstArtwork?.binaryData
        }.getOrNull()
        val bitmap = bytes?.let(::decode)
        if (bitmap == null) missing += key else cache[key] = bitmap
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
