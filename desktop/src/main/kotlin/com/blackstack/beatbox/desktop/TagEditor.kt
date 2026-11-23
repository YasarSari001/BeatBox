package com.blackstack.beatbox.desktop

import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import kotlin.math.pow

/**
 * Kullanıcı tarafından düzenlenebilen etiket alanları.
 *
 * Boş dize, alanın dosyada bulunmadığını gösterir; kaydetme sırasında boş
 * alanlar silinir.
 */
data class EditableTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
)

/**
 * MP3 (ID3v2), FLAC, OGG Vorbis, M4A/MP4, WAV ve AIFF etiketlerini okur/yazar.
 *
 * Jaudiotagger kullanılır; dosya biçimi desteklenmiyorsa işlem başarısız
 * döner ve kullanıcıya hata gösterilir.
 */
object TagEditor {

    /** ReplayGain parça kazancı (dB); etiket yoksa `null`. */
    fun replayGainDb(file: File): Float? = runCatching {
        val tag = AudioFileIO.read(file).tag ?: return@runCatching null
        val identifiers = listOf(
            "TXXX:REPLAYGAIN_TRACK_GAIN",
            "REPLAYGAIN_TRACK_GAIN",
            "TXXX:REPLAYGAIN_ALBUM_GAIN",
            "REPLAYGAIN_ALBUM_GAIN",
        )
        val raw = identifiers.firstNotNullOfOrNull { key ->
            runCatching { tag.getFirst(key)?.takeIf { it.isNotBlank() } }.getOrNull()
        } ?: return@runCatching null
        val match = Regex("""(-?\d+(?:[.,]\d+)?)""").find(raw) ?: return@runCatching null
        match.groupValues[1].replace(',', '.').toFloatOrNull()
    }.getOrNull()

    /** dB cinsinden kazancı PCM çarpanına çevirir (0,25× – 4× arası). */
    fun gainMultiplier(db: Float?): Float =
        if (db == null) 1f else (10.0.pow(db / 20.0)).toFloat().coerceIn(0.25f, 4f)

    /** Dosyadaki etiketleri okur; okunamazsa `null`. */
    fun read(file: File): EditableTags? = runCatching {
        val audio = AudioFileIO.read(file)
        val tag = audio.tagOrCreateAndSetDefault
        EditableTags(
            title = tag.readField(FieldKey.TITLE),
            artist = tag.readField(FieldKey.ARTIST),
            album = tag.readField(FieldKey.ALBUM),
            genre = tag.readField(FieldKey.GENRE),
            year = tag.readField(FieldKey.YEAR),
            trackNumber = tag.readField(FieldKey.TRACK),
        )
    }.getOrNull()

    /** Etiketleri yazar. Boş alanlar dosyadan silinir. */
    fun write(file: File, tags: EditableTags): Result<Unit> = runCatching {
        val audio = AudioFileIO.read(file)
        val tag = audio.tagOrCreateAndSetDefault
        tag.writeField(tags.title, FieldKey.TITLE)
        tag.writeField(tags.artist, FieldKey.ARTIST)
        tag.writeField(tags.album, FieldKey.ALBUM)
        tag.writeField(tags.genre, FieldKey.GENRE)
        tag.writeField(tags.year, FieldKey.YEAR)
        tag.writeField(tags.trackNumber, FieldKey.TRACK)
        audio.commit()
        Unit
    }

    private fun Tag.readField(key: FieldKey): String =
        runCatching { getFirst(key) }.getOrDefault("").orEmpty().trim()

    private fun Tag.writeField(value: String, key: FieldKey) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            runCatching { deleteField(key) }
        } else {
            setField(key, trimmed)
        }
    }
}
