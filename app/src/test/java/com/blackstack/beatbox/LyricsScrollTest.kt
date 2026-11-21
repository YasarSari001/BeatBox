package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import com.blackstack.beatbox.ui.lyrics.lyricsItemScrollOffset
import com.blackstack.beatbox.ui.lyrics.lyricsScrollDelta

/**
 * Şarkı sözü listesinin çapa noktasına tek geçişte kaydırılmasını doğrular.
 *
 * Önceki iki aşamalı akış (önce satırı tepeye çekip sonra geri indirme)
 * "yukarı sonra aşağı" titremesine yol açıyordu. Bu yardımcılar, hedef
 * konuma olan farkı tek bir değer olarak üretir.
 */
class LyricsScrollTest : StringSpec({

    "çapa noktasındaki satır için kaydırma gerekmez" {
        val delta = lyricsScrollDelta(
            itemOffset = 350,
            itemSize = 100,
            viewportStart = 0,
            viewportHeight = 1_000
        )

        delta shouldBe 0f
    }

    "çapanın üstündeki satır aşağı kaydırılır" {
        val delta = lyricsScrollDelta(
            itemOffset = 0,
            itemSize = 100,
            viewportStart = 0,
            viewportHeight = 1_000
        )

        // Satır merkezi 50 px, hedef 400 px: içerik 350 px aşağı iner.
        delta shouldBe -350f
    }

    "çapanın altındaki satır yukarı kaydırılır" {
        val delta = lyricsScrollDelta(
            itemOffset = 700,
            itemSize = 100,
            viewportStart = 0,
            viewportHeight = 1_000
        )

        // Satır merkezi 750 px, hedef 400 px: içerik 350 px yukarı çıkar.
        delta shouldBe 350f
    }

    "viewport başlangıcı sıfır değilken çapa buna göre hesaplanır" {
        val delta = lyricsScrollDelta(
            itemOffset = 450,
            itemSize = 100,
            viewportStart = 100,
            viewportHeight = 1_000
        )

        // Görünür alan 100..1100; hedef merkez 500 px; satır merkezi 500 px.
        delta shouldBe 0f
    }

    "özel çapa oranı hedefi değiştirir" {
        val delta = lyricsScrollDelta(
            itemOffset = 0,
            itemSize = 100,
            viewportStart = 0,
            viewportHeight = 1_000,
            anchorFraction = 0.5f
        )

        delta shouldBe -450f
    }

    "görünmeyen satır için offset satır merkezini çapaya getirir" {
        val offset = lyricsItemScrollOffset(
            estimatedItemSize = 100,
            viewportHeight = 1_000
        )

        // Satırın üst kenarı -350 px'e konur; merkez 400 px çapaya denk gelir.
        offset shouldBe -350
    }

    "görünmeyen satır offseti yuvarlanır" {
        val offset = lyricsItemScrollOffset(
            estimatedItemSize = 101,
            viewportHeight = 1_000
        )

        // 50.5 - 400 = -349.5; Kotlin yarım değerleri +sonsuza yuvarlar.
        offset shouldBe -349
    }

    "özel çapa oranı görünmeyen satır offsetini değiştirir" {
        val offset = lyricsItemScrollOffset(
            estimatedItemSize = 100,
            viewportHeight = 1_000,
            anchorFraction = 0.5f
        )

        offset shouldBe -450
    }
})
