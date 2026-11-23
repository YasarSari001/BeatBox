package com.blackstack.beatbox.desktop

import java.io.ByteArrayInputStream
import java.io.File
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TagEditorTest {

    @Test
    fun `writes and reads tags on a wav file`() {
        val file = File.createTempFile("beatbox-tag-", ".wav")
        try {
            writeSilentWav(file, seconds = 0.2)
            val tags = EditableTags(
                title = "Deneme Şarkı",
                artist = "BeatBox",
                album = "Test Albüm",
                genre = "Rock",
                year = "2026",
                trackNumber = "3",
            )

            val result = TagEditor.write(file, tags)
            assertTrue(result.isSuccess, "yazma hatası: ${result.exceptionOrNull()?.message}")

            val read = TagEditor.read(file)
            assertNotNull(read)
            assertEquals("Deneme Şarkı", read.title)
            assertEquals("BeatBox", read.artist)
            assertEquals("Test Albüm", read.album)
            assertEquals("Rock", read.genre)
            assertEquals("2026", read.year)
            assertEquals("3", read.trackNumber)
        } finally {
            file.delete()
        }
    }

    @Test
    fun `blank fields are removed`() {
        val file = File.createTempFile("beatbox-tag-clear-", ".wav")
        try {
            writeSilentWav(file, seconds = 0.1)
            TagEditor.write(file, EditableTags(title = "Eski", artist = "Eski Sanatçı"))
            val result = TagEditor.write(file, EditableTags(title = "Yeni"))
            assertTrue(result.isSuccess, "yazma hatası: ${result.exceptionOrNull()?.message}")

            val read = TagEditor.read(file)
            assertNotNull(read)
            assertEquals("Yeni", read.title)
            assertEquals("", read.artist)
        } finally {
            file.delete()
        }
    }

    private fun writeSilentWav(file: File, seconds: Double) {
        val format = AudioFormat(44100f, 16, 1, true, false)
        val frames = (44100 * seconds).toInt()
        val bytes = ByteArray(frames * 2)
        val stream = AudioInputStream(ByteArrayInputStream(bytes), format, frames.toLong())
        AudioSystem.write(stream, AudioFileFormat.Type.WAVE, file)
    }
}
