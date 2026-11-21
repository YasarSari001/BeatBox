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
package com.blackstack.beatbox.lyrics
/**
 * Parser for LRC (Lyrics) file format.
 *
 * Metrolist'in `lrcToMlx` ve `LrcTiming` uygulamalarından uyarlanan destekler:
 * - `[mm:ss]`, `[mm:ss.x]`, `[mm:ss.xx]`, `[mm:ss.xxx]` ve `[hh:mm:ss.xx]`
 * - Aynı satırda birden fazla zaman damgası (`[00:12.00][00:24.00]nakarat`)
 * - Büyük/küçük harfe duyarsız, işaretli `[offset:+500]` etiketi
 * - Gelişmiş LRC kelime zamanları (`<00:12.50>kelime`)
 * - "Synced by", "作词", "Composer:" gibi künye satırlarının atlanması
 */
object LRCParser {

    private val HOUR_TIMESTAMP =
        Regex("""\[(\d{1,3}):(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val LINE_TIMESTAMP =
        Regex("""\[(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val WORD_TIMESTAMP =
        Regex("""<(\d{1,3}):(\d{1,2})(?:[.:](\d{1,3}))?>""")
    private val METADATA_TAG =
        Regex("""^\[(ti|ar|al|by|offset|re|ve|length|au):(.*)]$""", RegexOption.IGNORE_CASE)
    private val CREDIT_PREFIX = Regex(
        """^(?:lyrics?|composer|composed by|written by|arranger|producer|mixed by|mastered by|""" +
            """作词|作曲|编曲|制作人|词曲|填词|混音|录音|监制)\s*[:：]""",
        RegexOption.IGNORE_CASE
    )
    private val SYNC_CREDIT = Regex("""^synced\s+by\b.*$""", RegexOption.IGNORE_CASE)

    /**
     * Parses LRC format string into [SyncedLyrics].
     *
     * @param lrcContent The LRC file content as a string
     * @return Result containing SyncedLyrics on success, or error on failure
     */
    fun parse(lrcContent: String): Result<SyncedLyrics> {
        return try {
            val lines = mutableListOf<SyncedLyricsLine>()
            val metadataMap = mutableMapOf<String, String>()

            lrcContent.lineSequence().forEach { rawLine ->
                val line = rawLine.trim()
                if (line.isEmpty()) return@forEach

                val metadataMatch = METADATA_TAG.matchEntire(line)
                if (metadataMatch != null) {
                    metadataMap[metadataMatch.groupValues[1].lowercase()] =
                        metadataMatch.groupValues[2].trim()
                    return@forEach
                }

                val timestamps = parseLeadingTimestamps(line)
                if (timestamps.times.isEmpty()) return@forEach

                val content = line.substring(timestamps.endIndex).trim()
                if (content.isEmpty()) return@forEach

                val parsedLine = parseEnhancedLine(content, timestamps.times.first())
                if (parsedLine.text.isBlank()) return@forEach
                if (isCreditLine(parsedLine.text)) return@forEach

                // Kelime zamanları yalnızca tek zaman damgalı satırlarda anlamlıdır;
                // çoklu damga çoğaltılan nakarat satırları için yalnızca satır zamanı tutulur.
                val words = if (timestamps.times.size == 1) parsedLine.words else emptyList()

                timestamps.times.forEach { timestamp ->
                    lines.add(
                        SyncedLyricsLine(
                            timestampMs = timestamp,
                            text = parsedLine.text,
                            words = words
                        )
                    )
                }
            }

            if (lines.none { it.text.isNotBlank() }) {
                return Result.failure(IllegalArgumentException("Lyrics content is empty"))
            }

            val metadata = LyricsMetadata(
                title = metadataMap["ti"],
                artist = metadataMap["ar"],
                album = metadataMap["al"],
                by = metadataMap["by"],
                offset = metadataMap["offset"]?.trim()?.toLongOrNull() ?: 0L,
            )

            val adjusted = lines
                .sortedBy { it.timestampMs }
                .map { line -> line.shiftedBy(metadata.offset) }

            Result.success(SyncedLyrics(adjusted, metadata))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Formats [SyncedLyrics] back to LRC string format.
     *
     * Offset, satır ve kelime zamanlarından geri çıkarılır; böylece
     * `parse(format(x))` aynı zamanlamayı üretir (çift offset uygulanmaz).
     */
    fun format(lyrics: SyncedLyrics): String {
        val builder = StringBuilder()
        val offset = lyrics.metadata.offset

        lyrics.metadata.title?.let { builder.appendLine("[ti:$it]") }
        lyrics.metadata.artist?.let { builder.appendLine("[ar:$it]") }
        lyrics.metadata.album?.let { builder.appendLine("[al:$it]") }
        lyrics.metadata.by?.let { builder.appendLine("[by:$it]") }
        if (offset != 0L) {
            builder.appendLine("[offset:$offset]")
        }

        if (builder.isNotEmpty()) {
            builder.appendLine()
        }

        lyrics.lines.forEach { line ->
            builder.append('[')
            builder.append(formatTimestamp((line.timestampMs - offset).coerceAtLeast(0L)))
            builder.append(']')

            if (line.words.isNotEmpty()) {
                line.words.forEach { word ->
                    builder.append('<')
                    builder.append(formatTimestamp((word.startMs - offset).coerceAtLeast(0L)))
                    builder.append('>')
                    builder.append(word.text)
                }
            } else {
                builder.append(line.text)
            }
            builder.appendLine()
        }

        return builder.toString()
    }

    private data class LeadingTimestamps(
        val times: List<Long>,
        val endIndex: Int,
    )

    /** Satır başındaki tüm `[...]` zaman damgalarını sırayla okur. */
    private fun parseLeadingTimestamps(line: String): LeadingTimestamps {
        val times = mutableListOf<Long>()
        var cursor = 0

        while (cursor < line.length) {
            val hourMatch = HOUR_TIMESTAMP.find(line, cursor)
                ?.takeIf { it.range.first == cursor }
            if (hourMatch != null) {
                times.add(
                    toMillis(
                        hours = hourMatch.groupValues[1],
                        minutes = hourMatch.groupValues[2],
                        seconds = hourMatch.groupValues[3],
                        fraction = hourMatch.groupValues[4]
                    )
                )
                cursor = hourMatch.range.last + 1
                continue
            }

            val lineMatch = LINE_TIMESTAMP.find(line, cursor)
                ?.takeIf { it.range.first == cursor }
                ?: break
            times.add(
                toMillis(
                    hours = null,
                    minutes = lineMatch.groupValues[1],
                    seconds = lineMatch.groupValues[2],
                    fraction = lineMatch.groupValues[3]
                )
            )
            cursor = lineMatch.range.last + 1
        }

        return LeadingTimestamps(times, cursor)
    }

    private data class ParsedLine(
        val text: String,
        val words: List<LyricsWord>,
    )

    /**
     * Gelişmiş LRC `<mm:ss.xx>` etiketlerini kelime zamanlarına çevirir.
     * Etiket yoksa metin olduğu gibi döner.
     */
    private fun parseEnhancedLine(content: String, lineTimeMs: Long): ParsedLine {
        val matches = WORD_TIMESTAMP.findAll(content).toList()
        if (matches.isEmpty()) {
            return ParsedLine(content.trim(), emptyList())
        }

        val words = mutableListOf<LyricsWord>()
        var cursor = 0

        matches.forEach { match ->
            if (match.range.first > cursor) {
                val prefix = content.substring(cursor, match.range.first)
                if (prefix.isNotEmpty()) {
                    val time = words.lastOrNull()?.startMs ?: lineTimeMs
                    words.add(LyricsWord(prefix, time))
                }
            }
            cursor = match.range.last + 1
            val time = toMillis(
                hours = null,
                minutes = match.groupValues[1],
                seconds = match.groupValues[2],
                fraction = match.groupValues[3]
            )
            words.add(LyricsWord("", time))
        }

        if (cursor < content.length) {
            val tail = content.substring(cursor)
            if (tail.isNotEmpty()) {
                if (words.isNotEmpty()) {
                    val last = words.removeAt(words.lastIndex)
                    words.add(last.copy(text = tail))
                } else {
                    words.add(LyricsWord(tail, 0L))
                }
            }
        }

        val nonEmptyWords = words.filter { it.text.isNotEmpty() }
        if (nonEmptyWords.isEmpty()) {
            return ParsedLine("", emptyList())
        }

        val rawText = nonEmptyWords.joinToString("") { it.text }
        val trimmedStart = rawText.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
        val trimmedEnd = rawText.indexOfLast { !it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
        val text = rawText.substring(trimmedStart, trimmedEnd)

        val adjustedWords = trimWordsToRange(nonEmptyWords, trimmedStart, trimmedEnd)
        return ParsedLine(text, adjustedWords)
    }

    /**
     * Baştaki/sondaki boşlukları metinden çıkarırken kelime sınırlarını
     * aynı aralığa taşır; böylece kelime uzunlukları toplamı metin
     * uzunluğuna eşit kalır.
     */
    private fun trimWordsToRange(
        words: List<LyricsWord>,
        start: Int,
        end: Int,
    ): List<LyricsWord> {
        val result = mutableListOf<LyricsWord>()
        var position = 0
        words.forEach { word ->
            val wordStart = position
            val wordEnd = position + word.text.length
            position = wordEnd

            val clippedStart = maxOf(wordStart, start)
            val clippedEnd = minOf(wordEnd, end)
            if (clippedStart < clippedEnd) {
                result.add(
                    word.copy(
                        text = word.text.substring(clippedStart - wordStart, clippedEnd - wordStart)
                    )
                )
            }
        }
        return result
    }

    private fun isCreditLine(text: String): Boolean =
        CREDIT_PREFIX.containsMatchIn(text) || SYNC_CREDIT.matches(text)

    private fun SyncedLyricsLine.shiftedBy(offsetMs: Long): SyncedLyricsLine {
        if (offsetMs == 0L) return this
        return copy(
            timestampMs = shift(timestampMs, offsetMs),
            words = words.map { it.copy(startMs = shift(it.startMs, offsetMs)) }
        )
    }

    private fun shift(valueMs: Long, offsetMs: Long): Long =
        (valueMs + offsetMs).coerceAtLeast(0L)

    private fun toMillis(
        hours: String?,
        minutes: String,
        seconds: String,
        fraction: String,
    ): Long {
        val h = hours?.toLongOrNull() ?: 0L
        val m = minutes.toLongOrNull() ?: 0L
        val s = seconds.toLongOrNull() ?: 0L
        val ms = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100L
            2 -> fraction.toLong() * 10L
            else -> fraction.take(3).toLong()
        }
        return ((h * 3600L + m * 60L + s) * 1000L) + ms
    }

    private fun formatTimestamp(timestampMs: Long): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (timestampMs % 1000) / 10

        return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
