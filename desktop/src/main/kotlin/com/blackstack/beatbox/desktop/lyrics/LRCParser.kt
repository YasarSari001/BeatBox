/*
 * BeatBox
 *
 * Bu dosya, GPL-3.0 lisansı altında yayımlanan Metrolist projesinden
 * (https://github.com/MetrolistGroup/Metrolist) uyarlanmıştır.
 * Uyarlanmış bu dosya da GPL-3.0 koşulları altında dağıtılır.
 *
 * This file is adapted from the Metrolist project, licensed under GPL-3.0.
 * This adapted file is distributed under the terms of GPL-3.0.
 */
package com.blackstack.beatbox.desktop.lyrics

/**
 * LRC (şarkı sözü) dosyası ayrıştırıcısı.
 *
 * Android sürümündeki `LRCParser` ile aynı kuralları uygular:
 * - `[mm:ss]`, `[mm:ss.x]`, `[mm:ss.xx]`, `[mm:ss.xxx]`, `[hh:mm:ss.xx]`
 * - Aynı satırda birden çok zaman damgası (`[00:12.00][00:24.00]nakarat`)
 * - Büyük/küçük harfe duyarsız `[offset:+500]`
 * - Gelişmiş LRC kelime zamanları (`<00:12.50>kelime`)
 * - "Synced by", "作词", "Composer:" gibi künye satırlarının atlanması
 */
object LRCParser {

    private val HOUR_TIMESTAMP =
        Regex("""\[(\d{1,3}):(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val LINE_TIMESTAMP =
        Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TIMESTAMP = Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val META_TAG = Regex("""^\[(ti|ar|al|by|offset|length|re|ve):(.*)]$""", RegexOption.IGNORE_CASE)
    private val CREDIT_LINE = Regex(
        """^(synced\s*by|lyrics?\s*by|composer|writer|作词|作曲|编曲|制作人|混音)\s*[:：]?""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * Ham LRC metnini [SyncedLyrics]'e çevirir. Söz bulunamazsa `null` döner.
     */
    fun parse(raw: String, source: String? = null): SyncedLyrics? {
        if (raw.isBlank()) return null

        var title: String? = null
        var artist: String? = null
        var album: String? = null
        var by: String? = null
        var offsetMs = 0L
        val entries = mutableListOf<SyncedLyricsLine>()

        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val meta = META_TAG.find(line)
            if (meta != null) {
                val value = meta.groupValues[2].trim()
                when (meta.groupValues[1].lowercase()) {
                    "ti" -> title = value.ifBlank { null }
                    "ar" -> artist = value.ifBlank { null }
                    "al" -> album = value.ifBlank { null }
                    "by" -> by = value.ifBlank { null }
                    "offset" -> offsetMs = value.replace("+", "").toLongOrNull() ?: 0L
                }
                continue
            }

            // Satırdaki tüm ana zaman damgalarını topla.
            val times = mutableListOf<Long>()
            var remainder = line
            while (true) {
                val hour = HOUR_TIMESTAMP.find(remainder)
                val simple = LINE_TIMESTAMP.find(remainder)
                val hourFirst = hour != null &&
                    (simple == null || hour.range.first <= simple.range.first)

                if (hourFirst && hour != null) {
                    times += toMillis(
                        hour.groupValues[1].toLongOrNull() ?: 0L,
                        hour.groupValues[2].toLongOrNull() ?: 0L,
                        hour.groupValues[3].toLongOrNull() ?: 0L,
                        hour.groupValues[4],
                    )
                    remainder = remainder.removeRange(hour.range)
                } else if (simple != null) {
                    times += toMillis(
                        0L,
                        simple.groupValues[1].toLongOrNull() ?: 0L,
                        simple.groupValues[2].toLongOrNull() ?: 0L,
                        simple.groupValues[3],
                    )
                    remainder = remainder.removeRange(simple.range)
                } else {
                    break
                }
            }

            if (times.isEmpty()) continue
            if (CREDIT_LINE.containsMatchIn(remainder.trim())) continue

            val (text, words) = parseWords(remainder.trim())
            if (text.isBlank()) continue

            for (timestamp in times) {
                entries += SyncedLyricsLine(
                    timestampMs = (timestamp + offsetMs).coerceAtLeast(0L),
                    text = text,
                    words = words,
                )
            }
        }

        if (entries.isEmpty()) return null

        val sorted = entries.sortedBy { it.timestampMs }
        return SyncedLyrics(
            lines = sorted,
            metadata = LyricsMetadata(
                title = title,
                artist = artist,
                album = album,
                by = by,
                offset = offsetMs,
                source = source,
            ),
        )
    }

    /**
     * Gelişmiş LRC satırındaki `<mm:ss.xx>` kelime etiketlerini ayıklar.
     * Etiket yoksa düz metin ve boş kelime listesi döner.
     */
    private fun parseWords(line: String): Pair<String, List<LyricsWord>> {
        val matches = WORD_TIMESTAMP.findAll(line).toList()
        if (matches.isEmpty()) return line to emptyList()

        val words = mutableListOf<LyricsWord>()
        val plain = StringBuilder()
        var cursor = 0
        for ((index, match) in matches.withIndex()) {
            if (match.range.first > cursor) {
                plain.append(line, cursor, match.range.first)
            }
            val start = toMillis(
                0L,
                match.groupValues[1].toLongOrNull() ?: 0L,
                match.groupValues[2].toLongOrNull() ?: 0L,
                match.groupValues[3],
            )
            val end = if (index + 1 < matches.size) matches[index + 1].range.first else line.length
            val wordText = line.substring(match.range.last + 1, end)
            if (wordText.isNotEmpty()) {
                words += LyricsWord(text = wordText, startMs = start)
                plain.append(wordText)
            }
            cursor = end
        }
        if (cursor < line.length) plain.append(line, cursor, line.length)
        return plain.toString().trim() to words
    }

    private fun toMillis(hours: Long, minutes: Long, seconds: Long, fraction: String): Long {
        val fractionMs = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLongOrNull()?.times(100) ?: 0L
            2 -> fraction.toLongOrNull()?.times(10) ?: 0L
            else -> fraction.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        }
        return ((hours * 3600 + minutes * 60 + seconds) * 1000) + fractionMs
    }
}
