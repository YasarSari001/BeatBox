package com.blackstack.beatbox.desktop

import com.blackstack.beatbox.metadata.cleanDisplayTitle
import com.blackstack.beatbox.metadata.parseFilenameMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Kütüphanedeki medya türü. */
enum class MediaKind { AUDIO, VIDEO, IMAGE }

/** Kütüphanedeki tek dosya. */
data class Track(
    val file: File,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationSeconds: Double,
    val modifiedAt: Long = 0L,
    val kind: MediaKind = MediaKind.AUDIO,
    val sizeBytes: Long = 0L,
) {
    val folderName: String get() = file.parentFile?.name.orEmpty()
}

/** Java Sound + SPI sağlayıcılarının açabildiği ses biçimleri. */
val audioExtensions = setOf("mp3", "wav", "ogg", "aiff", "aif", "au")

/** FFmpeg'in açabildiği video biçimleri. */
val videoExtensions = setOf("mp4", "m4v", "mkv", "webm", "mov", "avi", "3gp", "wmv", "flv", "mpg", "mpeg", "ts")

/** ImageIO'nun açabildiği fotoğraf biçimleri. */
val imageExtensions = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "jfif")

/**
 * Klasörü alt klasörleriyle tarar.
 *
 * Ses dosyalarının başlık/sanatçısı önce gömülü etiketten, yoksa dosya
 * adından çıkarılır ve süresi ölçülür; video ve fotoğraflar için süre
 * ölçülmez (yüzlerce dosyada taramayı yavaşlatırdı), video süresi
 * oynatılırken okunur.
 */
suspend fun scanFolder(root: File, engine: AudioEngine): List<Track> = withContext(Dispatchers.IO) {
    if (!root.isDirectory) return@withContext emptyList()

    root.walkTopDown()
        .onEnter { dir -> !dir.name.startsWith(".") }
        // Gizli dosyalar ve WhatsApp'ın ".trashed-" kalıntıları atlanır.
        .filter { it.isFile && !it.name.startsWith(".") }
        .mapNotNull { file ->
            val extension = file.extension.lowercase()
            val kind = when (extension) {
                in audioExtensions -> MediaKind.AUDIO
                in videoExtensions -> MediaKind.VIDEO
                in imageExtensions -> MediaKind.IMAGE
                else -> return@mapNotNull null
            }
            val rawName = file.nameWithoutExtension
            val parsed = parseFilenameMetadata(rawName)
            val embedded = if (kind == MediaKind.AUDIO) TagEditor.read(file) else null
            val embeddedTitle = embedded?.title?.takeIf { it.isNotBlank() }
            val embeddedArtist = embedded?.artist?.takeIf { it.isNotBlank() }
            Track(
                file = file,
                title = embeddedTitle ?: cleanDisplayTitle(parsed?.title ?: rawName),
                artist = embeddedArtist ?: parsed?.artist.orEmpty(),
                album = embedded?.album.orEmpty(),
                durationSeconds = if (kind == MediaKind.AUDIO) engine.durationSeconds(file) else 0.0,
                modifiedAt = file.lastModified(),
                kind = kind,
                sizeBytes = file.length(),
            )
        }
        .sortedWith(compareBy({ it.artist.lowercase() }, { it.title.lowercase() }))
        .toList()
}

/** Saniyeyi "3:07" biçimine çevirir. */
fun formatTime(seconds: Double): String {
    if (seconds.isNaN() || seconds <= 0.0) return "0:00"
    val total = seconds.toLong().coerceAtLeast(0)
    val minutes = total / 60
    val secs = total % 60
    return "%d:%02d".format(minutes, secs)
}

/** Dosya boyutu (fotoğraf ve videolar için süre yerine gösterilir). */
fun formatSize(bytes: Long): String {
    if (bytes <= 0L) return "-"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    return when {
        mb >= 1.0 -> "%.1f MB".format(mb)
        else -> "%.0f KB".format(kb)
    }
}
