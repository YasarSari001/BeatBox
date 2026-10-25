package com.blackstack.beatbox

/**
 * Dosya adından çıkarılan başlık ve sanatçı.
 *
 * [artist] null ise dosya adında sanatçı bilgisi yoktur; mevcut ID3 sanatçısı
 * korunmalıdır.
 */
data class FilenameMetadata(
    val title: String,
    val artist: String?
)

private val TRACK_PREFIX = Regex("^\\d{1,3}\\s*[-._]\\s+")

/**
 * "Sanatçı - Başlık.mp3" veya "Başlık.mp3" biçimini çözer.
 *
 * - Uzantı çağıran tarafından çıkarılır.
 * - "01 - ", "01. ", "01_ " gibi parça numarası önekleri atılır.
 * - Kısa/uzun çizgi ayırıcıları normal tireye çevrilir.
 * - Birden fazla " - " varsa yalnızca ilki sanatçı sınırıdır; kalanı başlığın
 *   parçasıdır ("Artist - Title - Live").
 * - Başlık boşsa null döner; böylece bozuk dosya adı etiketi ezmez.
 */
fun parseFilenameMetadata(rawName: String): FilenameMetadata? {
    var name = rawName.trim()
    if (name.isEmpty()) return null

    val withoutTrack = TRACK_PREFIX.replaceFirst(name, "")
    if (withoutTrack != name && withoutTrack.isNotBlank()) {
        name = withoutTrack.trim()
    }

    val normalized = name
        .replace(" \u2013 ", " - ") // en dash
        .replace(" \u2014 ", " - ") // em dash

    // "Sanatçı -" gibi başlığı boş bırakan adlar etiketi ezmemeli.
    if (normalized.endsWith(" -") || normalized.endsWith(" \u2013") ||
        normalized.endsWith(" \u2014")
    ) {
        return null
    }

    val separator = normalized.indexOf(" - ")
    if (separator <= 0) {
        return FilenameMetadata(title = normalized, artist = null)
    }

    val artist = normalized.substring(0, separator).trim()
    val title = normalized.substring(separator + 3).trim()
    if (title.isEmpty()) return null

    return FilenameMetadata(
        title = title,
        artist = artist.ifEmpty { null }
    )
}

/**
 * ID3 etiketinin yeniden yazılması gerekip gerekmediğini söyler.
 *
 * MediaStore satırı dosya adıyla aynı değerleri taşıyorsa yazma atlanır; böylece
 * her yenilemede yüzlerce dosya yeniden yazılmaz.
 */
fun needsMetadataUpdate(
    currentTitle: String,
    currentArtist: String,
    parsed: FilenameMetadata
): Boolean {
    if (currentTitle != parsed.title) return true
    val artist = parsed.artist ?: return false
    return currentArtist != artist
}
