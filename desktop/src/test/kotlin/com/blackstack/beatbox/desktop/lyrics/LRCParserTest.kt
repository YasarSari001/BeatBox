package com.blackstack.beatbox.desktop.lyrics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LRCParserTest {

    @Test
    fun `sorts lines and parses centisecond timestamps`() {
        val lyrics = LRCParser.parse(
            """
            [00:12.34]ikinci satır
            [00:02.10]ilk satır
            """.trimIndent(),
            "test",
        )

        assertNotNull(lyrics)
        assertEquals(listOf(2100L, 12340L), lyrics.lines.map { it.timestampMs })
        assertEquals("ilk satır", lyrics.lines.first().text)
        assertEquals("test", lyrics.metadata.source)
        assertTrue(lyrics.synced)
    }

    @Test
    fun `applies multiple timestamps on one line`() {
        val lyrics = LRCParser.parse("[00:10.00][00:30.50]nakarat", "test")

        assertNotNull(lyrics)
        assertEquals(listOf(10000L, 30500L), lyrics.lines.map { it.timestampMs })
        assertEquals(listOf("nakarat", "nakarat"), lyrics.lines.map { it.text })
    }

    @Test
    fun `reads metadata and offset`() {
        val lyrics = LRCParser.parse(
            """
            [ti:Deneme]
            [ar:BeatBox]
            [offset:+250]
            [00:01.00]bir
            """.trimIndent(),
            "test",
        )

        assertNotNull(lyrics)
        assertEquals("Deneme", lyrics.metadata.title)
        assertEquals("BeatBox", lyrics.metadata.artist)
        assertEquals(250L, lyrics.metadata.offset)
        assertEquals(1250L, lyrics.lines.single().timestampMs)
    }

    @Test
    fun `supports hour timestamps`() {
        val lyrics = LRCParser.parse("[01:02:03.500]uzun parça", "test")

        assertNotNull(lyrics)
        assertEquals(3723500L, lyrics.lines.single().timestampMs)
    }

    @Test
    fun `parses enhanced word timings`() {
        val lyrics = LRCParser.parse("[00:05.00]<00:05.00>Merhaba <00:06.20>dünya", "test")

        assertNotNull(lyrics)
        val line = lyrics.lines.single()
        assertEquals("Merhaba dünya", line.text)
        assertEquals(listOf(5000L, 6200L), line.words.map { it.startMs })
        assertEquals(listOf("Merhaba ", "dünya"), line.words.map { it.text })
    }

    @Test
    fun `skips credit lines`() {
        val lyrics = LRCParser.parse(
            """
            [00:00.00]Synced by BeatBox
            [00:01.00]gerçek satır
            """.trimIndent(),
            "test",
        )

        assertNotNull(lyrics)
        assertEquals(listOf("gerçek satır"), lyrics.lines.map { it.text })
    }

    @Test
    fun `returns null for text without timestamps`() {
        assertNull(LRCParser.parse("sadece düz metin", "test"))
    }

    @Test
    fun `provider wraps plain text as unsynced lyrics`() {
        val lyrics = LyricsProvider.fromText("birinci\nikinci\n", "Lyrics.ovh")

        assertNotNull(lyrics)
        assertFalse(lyrics.synced)
        assertEquals(listOf("birinci", "ikinci"), lyrics.lines.map { it.text })
        assertEquals("Lyrics.ovh", lyrics.metadata.source)
    }

    @Test
    fun `provider prefers synced parsing when timestamps exist`() {
        val lyrics = LyricsProvider.fromText("[00:01.00]bir\n[00:02.00]iki", "LRCLIB")

        assertNotNull(lyrics)
        assertTrue(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
    }
}
