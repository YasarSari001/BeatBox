package com.blackstack.beatbox.metadata
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
 * Uygulama artık etiket yazmadığı için bu karşılaştırma yalnızca başlık ve
 * sanatçının dosya adıyla aynı olup olmadığını anlatır; görünüm katmanı
 * gereksiz kopya üretmemek için kullanabilir.
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

/** Parantez, köşeli parantez veya süslü parantez içindeki gruplar. */
private val bracketGroup = Regex("""[\(\[\{]([^\)\]\}]*)[\)\]\}]""")

/** Başlığın sonunda kalan "- Official Video" gibi kuyruklar. */
private val noiseTail = Regex(
    """(?i)\s*[-–—|/]\s*((official\s+)?(music\s+)?(video|audio|lyric[s]?|visualizer)|official)\s*$"""
)

private val repeatedSpace = Regex("""\s{2,}""")

/** Yalnızca dağıtım etiketi taşıyan parantez gruplarını tanımak için. */
private val distributionTokens = setOf(
    "official", "audio", "video", "music", "lyric", "lyrics", "visualizer",
    "mv", "m", "v", "hd", "hq", "4k", "8k"
)

private fun isDistributionTag(inner: String): Boolean {
    val words = inner.lowercase()
        .replace(".", " ")
        .split(' ', '/', '&')
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    return words.isNotEmpty() && words.all { it in distributionTokens }
}

/**
 * Görünen başlığı dağıtım etiketlerinden arındırır.
 *
 * "(Official Video)", "[Official Audio]", "(Lyrics)", "(Visualizer)" gibi
 * yalnızca tanıtım amacı taşıyan parantez grupları ve sondaki "Official Video"
 * kuyrukları atılır. Sanatçı ve "(Live)", "(Remix)", "(feat. ...)" gibi içerik
 * belirten gruplar korunur.
 *
 * Bu yalnızca uygulama görünümünde kullanılır; dosya etiketi değiştirilmez.
 */
fun cleanDisplayTitle(raw: String): String {
    if (raw.isBlank()) return raw

    var title = bracketGroup.replace(raw) { match ->
        if (isDistributionTag(match.groupValues[1])) " " else match.value
    }
    // "Official Video | Official Audio" gibi çoklu kuyruklar için iki tur.
    repeat(2) { title = title.replace(noiseTail, "") }

    title = title.replace(repeatedSpace, " ").trim()
    while (title.isNotEmpty() && title.last() in " -\u2013\u2014|/_") {
        title = title.dropLast(1).trim()
    }

    return title.ifBlank { raw.trim() }
}
