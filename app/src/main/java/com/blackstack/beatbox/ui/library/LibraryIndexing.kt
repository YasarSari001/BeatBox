package com.blackstack.beatbox.ui.library

import com.blackstack.beatbox.AudioFile
import java.text.Normalizer
import java.util.Locale

/**
 * Kütüphane listesini alfabetik bölümlere ayıran saf yardımcılar.
 *
 * Türkçe ve diğer Latin dillerindeki aksanlı harfler ASCII karşılıklarına
 * indirgenir; böylece "Çilek" C bölümünde, "İstanbul" I bölümünde görünür.
 */

private val diacriticMarks = Regex("\\p{Mn}+")

/** Sıralama için küçük harfli, aksansız anahtar üretir. */
internal fun librarySortKey(value: String): String =
    Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        .replace(diacriticMarks, "")
        .lowercase(Locale.ROOT)

/**
 * Başlığın ait olduğu A-Z bölümünü döndürür.
 * Latin alfabesine indirgenemeyen başlıklar '#' bölümünde toplanır.
 */
internal fun libraryLetter(title: String): Char {
    val first = title.trim().firstOrNull() ?: return '#'
    if (first.isDigit()) return '#'
    val normalized = Normalizer.normalize(first.toString(), Normalizer.Form.NFD)
        .replace(diacriticMarks, "")
        .uppercase(Locale.ROOT)
    return normalized.firstOrNull()?.takeIf { it in 'A'..'Z' } ?: '#'
}

/** Sıralama sırasında öğe başına bir kez hesaplanan anahtarı taşır. */
private class SortEntry<T>(val item: T, val key: String, val raw: String)

/**
 * [selector]'dan üretilen aksansız anahtara göre sıralar.
 *
 * Anahtar her öğe için bir kez hesaplanır. Daha önce `compareBy` içinde
 * hesaplandığı için `Normalizer` ve regex her karşılaştırmada çalışıyordu
 * (O(n log n) kez); binlerce şarkılı bir kitaplıkta sekme geçişleri bu yüzden
 * yüzlerce milisaniye kilitlenebiliyordu.
 */
internal fun <T> sortedByLibraryKey(items: List<T>, selector: (T) -> String): List<T> {
    if (items.size < 2) return items
    return items
        .map { item ->
            val raw = selector(item)
            SortEntry(item, librarySortKey(raw), raw)
        }
        .sortedWith(compareBy<SortEntry<T>> { it.key }.thenBy { it.raw })
        .map { it.item }
}

/** Başlıkları aksan duyarsız biçimde A-Z sıralar. */
internal fun sortedByTitle(songs: List<AudioFile>): List<AudioFile> =
    sortedByLibraryKey(songs) { it.title }
