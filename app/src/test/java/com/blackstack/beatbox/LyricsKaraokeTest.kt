package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import com.blackstack.beatbox.lyrics.LyricsWord
import com.blackstack.beatbox.ui.lyrics.karaokeSungChars
import com.blackstack.beatbox.ui.lyrics.karaokeSungCharsForWords

/**
 * Karaoke renklendirmesinin karakter sınırlarını doğrular.
 *
 * Sınır hataları, satırın sonunda taşma veya hiç renklenmeme olarak görünür.
 */
class LyricsKaraokeTest : StringSpec({

    "boş metin için sıfır karakter" {
        karaokeSungChars(textLength = 0, progress = 0.5f) shouldBe 0
    }

    "ilerleme sıfırken hiç karakter boyanmaz" {
        karaokeSungChars(textLength = 12, progress = 0f) shouldBe 0
    }

    "ilerleme yarıdayken metnin yarısı boyanır" {
        karaokeSungChars(textLength = 12, progress = 0.5f) shouldBe 6
    }

    "ilerleme birden büyükse metnin tamamı boyanır" {
        karaokeSungChars(textLength = 12, progress = 1.8f) shouldBe 12
    }

    "negatif ilerleme sıfıra sabitlenir" {
        karaokeSungChars(textLength = 12, progress = -0.4f) shouldBe 0
    }

    "kelime zamanı yoksa sıfır karakter" {
        karaokeSungCharsForWords(
            words = emptyList(),
            positionMs = 5_000L,
            textLength = 10,
            lineEndMs = 10_000L
        ) shouldBe 0
    }

    "ilk kelimeden önce hiçbir karakter boyanmaz" {
        val words = listOf(
            LyricsWord("Hello ", 1_000L),
            LyricsWord("world", 2_000L)
        )

        karaokeSungCharsForWords(words, 500L, 11, 3_000L) shouldBe 0
    }

    "ilk kelimenin yarısında kelimenin yarısı boyanır" {
        val words = listOf(
            LyricsWord("Hello ", 1_000L),
            LyricsWord("world", 2_000L)
        )

        // "Hello " altı karakter; 1500 ms kelimenin tam ortası.
        karaokeSungCharsForWords(words, 1_500L, 11, 3_000L) shouldBe 3
    }

    "ikinci kelime başladığında ilk kelime tamamen boyanır" {
        val words = listOf(
            LyricsWord("Hello ", 1_000L),
            LyricsWord("world", 2_000L)
        )

        karaokeSungCharsForWords(words, 2_000L, 11, 3_000L) shouldBe 6
    }

    "satır sonunda tüm metin boyanır" {
        val words = listOf(
            LyricsWord("Hello ", 1_000L),
            LyricsWord("world", 2_000L)
        )

        karaokeSungCharsForWords(words, 3_000L, 11, 3_000L) shouldBe 11
    }

    "kelime zamanlaması metin uzunluğunu aşmaz" {
        val words = listOf(
            LyricsWord("Hello ", 1_000L),
            LyricsWord("world", 2_000L)
        )

        karaokeSungCharsForWords(words, 99_999L, 11, 3_000L) shouldBe 11
    }

    "boş metin için kelime zamanlaması sıfır döner" {
        karaokeSungCharsForWords(
            words = listOf(LyricsWord("Hello", 0L)),
            positionMs = 500L,
            textLength = 0,
            lineEndMs = 1_000L
        ) shouldBe 0
    }
})
