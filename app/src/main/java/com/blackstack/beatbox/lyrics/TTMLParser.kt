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
import kotlin.math.roundToLong

/**
 * TTML (Timed Text Markup Language) ayrıştırıcısı.
 *
 * Metrolist'in `parseTtmlToMlx` uygulamasından uyarlanmıştır. BeatBox, MLX
 * yerine LRC üretir; TTML içindeki `<span begin="...">` kelime zamanlamaları
 * gelişmiş LRC (`<mm:ss.xx>kelime`) etiketlerine dönüştürülür. Böylece
 * BetterLyrics, lrc.red ve Unison gibi TTML sağlayıcıları kelime bazlı
 * karaoke desteği kazanır.
 */
object TTMLParser {

    private val P_TAG = Regex(
        """<p\b([^>]*)>(.*?)</p\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val SPAN_TAG = Regex("""<(/?)span\b([^>]*)>""", RegexOption.IGNORE_CASE)
    private val ATTRIBUTE = Regex("""([\w:.-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val ANY_TAG = Regex("""<[^>]*>""")
    private val BR_TAG = Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE)
    private val TRANSLATION_BLOCK = Regex(
        """<span\b[^>]*(?:ttm:role|role)\s*=\s*["']x-(?:translation|romanization)["'][^>]*>.*?</span\s*>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val NUMERIC_ENTITY = Regex("""&#(x?[0-9a-fA-F]+);""")

    /** TTML içindeki tek bir kelimenin zaman damgası. */
    data class TTMLWord(
        val text: String,
        val timestampMs: Long
    )

    /**
     * Tek bir şarkı sözü satırı.
     *
     * @property words Boş değilse kelime metinlerinin birleşimi [text] ile aynıdır.
     */
    data class TTMLNode(
        val timestampMs: Long,
        val text: String,
        val words: List<TTMLWord> = emptyList()
    )

    /**
     * TTML belgesini satır listesine çevirir.
     *
     * Çeviri/romanizasyon blokları atlanır; `<br/>` etiketleri boşluğa
     * dönüştürülür. Kelime zamanlamaları satır metniyle hizalanamazsa
     * yalnızca satır zamanlaması korunur.
     */
    fun parseTTML(ttmlContent: String): List<TTMLNode> {
        if (ttmlContent.isBlank()) return emptyList()

        val cleaned = ttmlContent
            .replace(TRANSLATION_BLOCK, "")
            .replace(BR_TAG, " ")

        val nodes = mutableListOf<TTMLNode>()
        try {
            P_TAG.findAll(cleaned).forEach { match ->
                val attributes = parseAttributes(match.groupValues[1])
                val role = roleOf(attributes)
                if (role == "x-translation" || role == "x-romanization") {
                    return@forEach
                }

                val content = match.groupValues[2]
                val text = decodeEntities(ANY_TAG.replace(content, ""))
                    .replace(Regex("""\s+"""), " ")
                    .trim()
                if (text.isEmpty()) return@forEach

                val words = parseWords(content)
                val lineStart = parseTime(attributes["begin"])
                    ?: words.firstOrNull()?.timestampMs
                    ?: return@forEach
                val alignedWords = if (words.size >= 2) alignWords(words, text) else emptyList()

                nodes.add(
                    TTMLNode(
                        timestampMs = lineStart,
                        text = text,
                        words = alignedWords
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return nodes.sortedBy { it.timestampMs }
    }

    /**
     * Satırları LRC biçimine çevirir.
     *
     * Kelime zamanlaması varsa gelişmiş LRC üretilir; LRCParser bu biçimi
     * okuyup karaoke vurgusunu kelime sınırlarına oturtur.
     */
    fun toLRC(nodes: List<TTMLNode>): String {
        val builder = StringBuilder()
        nodes.forEach { node ->
            builder.append('[')
            builder.append(formatTimestamp(node.timestampMs))
            builder.append(']')

            if (node.words.isNotEmpty()) {
                node.words.forEach { word ->
                    builder.append('<')
                    builder.append(formatTimestamp(word.timestampMs))
                    builder.append('>')
                    builder.append(word.text)
                }
            } else {
                builder.append(node.text)
            }
            builder.appendLine()
        }
        return builder.toString()
    }

    private data class SpanFrame(
        val attributes: Map<String, String>,
        val contentStart: Int,
        var hasChild: Boolean = false
    )

    /** Bir `<p>` içindeki yaprak `<span>` etiketlerinden kelime listesi çıkarır. */
    private fun parseWords(content: String): List<TTMLWord> {
        val words = mutableListOf<TTMLWord>()
        val stack = ArrayDeque<SpanFrame>()

        for (match in SPAN_TAG.findAll(content)) {
            val isClosing = match.groupValues[1].isNotEmpty()
            if (!isClosing) {
                val frame = SpanFrame(
                    attributes = parseAttributes(match.groupValues[2]),
                    contentStart = match.range.last + 1
                )
                stack.addLast(frame)
                if (stack.size > 1) {
                    stack[stack.size - 2].hasChild = true
                }
                continue
            }

            val frame = stack.removeLastOrNull() ?: continue
            if (frame.hasChild) continue

            val role = roleOf(frame.attributes)
            if (role == "x-translation" || role == "x-romanization") continue

            val begin = parseTime(frame.attributes["begin"]) ?: continue
            val raw = content.substring(frame.contentStart, match.range.first)
            val text = decodeEntities(ANY_TAG.replace(raw, "")).trim()
            if (text.isNotEmpty()) {
                words.add(TTMLWord(text, begin))
            }
        }
        return words
    }

    /**
     * Kelime metinlerini satır metnine hizalar.
     *
     * TTML'de kelimeler arası boşluklar bazen span dışında kalır. Hizalama
     * başarısız olursa boş liste döner ve satır yalnızca satır zamanlamasıyla
     * gösterilir; metin asla bozulmaz.
     */
    private fun alignWords(words: List<TTMLWord>, fullText: String): List<TTMLWord> {
        val result = mutableListOf<TTMLWord>()
        var cursor = 0

        for (word in words) {
            val core = word.text.filterNot { it.isWhitespace() }
            if (core.isEmpty()) continue

            var scan = cursor
            var matched = 0
            val start = cursor
            while (scan < fullText.length && matched < core.length) {
                val character = fullText[scan]
                if (character.isWhitespace()) {
                    scan++
                    continue
                }
                if (character != core[matched]) return emptyList()
                matched++
                scan++
            }
            if (matched < core.length) return emptyList()

            var end = scan
            while (end < fullText.length && fullText[end].isWhitespace()) end++
            result.add(word.copy(text = fullText.substring(start, end)))
            cursor = end
        }

        if (cursor < fullText.length && fullText.substring(cursor).isNotBlank()) {
            return emptyList()
        }
        return result
    }

    private fun parseAttributes(raw: String): Map<String, String> =
        ATTRIBUTE.findAll(raw).associate { match ->
            val key = match.groupValues[1].lowercase()
            val value = match.groupValues[2].ifEmpty { match.groupValues[3] }
            key to value
        }

    private fun roleOf(attributes: Map<String, String>): String? =
        attributes.entries
            .firstOrNull { it.key == "role" || it.key.endsWith(":role") }
            ?.value
            ?.lowercase()

    private fun decodeEntities(raw: String): String {
        var text = raw
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&nbsp;", " ")

        text = NUMERIC_ENTITY.replace(text) { match ->
            val value = match.groupValues[1]
            val code = if (value.startsWith("x", ignoreCase = true)) {
                value.drop(1).toIntOrNull(16)
            } else {
                value.toIntOrNull()
            }
            code?.takeIf { it in 1..0x10FFFF }?.let { String(Character.toChars(it)) } ?: ""
        }
        return text
    }

    /**
     * TTML zaman ifadelerini milisaniyeye çevirir.
     *
     * Desteklenen biçimler: `12.345`, `12.345s`, `00:12.345`, `00:00:12.345`.
     */
    private fun parseTime(raw: String?): Long? {
        val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return try {
            if (!value.contains(":") && value.endsWith("s", ignoreCase = true)) {
                val seconds = value.dropLast(1).toDoubleOrNull() ?: return null
                return (seconds * 1000.0).roundToLong().coerceAtLeast(0L)
            }

            val parts = value.split(":")
            when (parts.size) {
                1 -> parts[0].toDoubleOrNull()
                    ?.let { (it * 1000.0).roundToLong().coerceAtLeast(0L) }

                2 -> {
                    val minutes = parts[0].toLongOrNull() ?: return null
                    val seconds = parts[1].toDoubleOrNull() ?: return null
                    ((minutes * 60 + seconds) * 1000.0).roundToLong().coerceAtLeast(0L)
                }

                3 -> {
                    val hours = parts[0].toLongOrNull() ?: return null
                    val minutes = parts[1].toLongOrNull() ?: return null
                    val seconds = parts[2].toDoubleOrNull() ?: return null
                    ((hours * 3600 + minutes * 60 + seconds) * 1000.0)
                        .roundToLong()
                        .coerceAtLeast(0L)
                }

                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTimestamp(timestampMs: Long): String {
        val totalSeconds = timestampMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (timestampMs % 1000) / 10
        return "%02d:%02d.%02d".format(minutes, seconds, centiseconds)
    }
}
