package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

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
})
