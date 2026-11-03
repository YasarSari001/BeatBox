package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Kapak decode'larında kullanılan örnekleme katsayısının birim testleri.
 * Büyük kapakların tam çözünürlükte belleğe alınmasını engeller.
 */
class ImageSamplingTest : StringSpec({

    "3000x3000 kapak 256 hedefte 8 kat örneklenir" {
        calculateInSampleSize(3000, 3000, 256) shouldBe 8
    }

    "1000x1000 kapak 256 hedefte 2 kat örneklenir" {
        calculateInSampleSize(1000, 1000, 256) shouldBe 2
    }

    "hedef boyuttaki kapak örneklenmez" {
        calculateInSampleSize(512, 512, 256) shouldBe 1
    }

    "hedeften küçük kapak büyütülmez" {
        calculateInSampleSize(100, 100, 256) shouldBe 1
    }

    "geniş kapakta uzun kenar sınırı korunur" {
        calculateInSampleSize(4000, 2000, 512) shouldBe 4
    }

    "8000 px kapak 512 hedefte 8 kat örneklenir" {
        calculateInSampleSize(8192, 8192, 512) shouldBe 8
    }

    "dikdörtgen kapakta da örnekleme uygulanır" {
        calculateInSampleSize(2000, 1000, 256) shouldBe 4
    }

    "sıfır hedefte bile geçerli bir katsayı döner" {
        calculateInSampleSize(100, 100, 0) shouldBe 64
    }
})
