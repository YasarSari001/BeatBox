package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import com.blackstack.beatbox.metadata.FilenameMetadata
import com.blackstack.beatbox.metadata.needsMetadataUpdate
import com.blackstack.beatbox.metadata.parseFilenameMetadata

/**
 * Dosya adı çözümlemesinin ve gereksiz ID3 yazımını engelleyen karşılaştırmanın
 * birim testleri.
 */
class FilenameMetadataTest : StringSpec({

    "sanatçı ve başlık tire ile ayrılır" {
        val parsed = parseFilenameMetadata("Daft Punk - Get Lucky")

        parsed?.artist shouldBe "Daft Punk"
        parsed?.title shouldBe "Get Lucky"
    }

    "sanatçısız dosya adında yalnızca başlık döner" {
        val parsed = parseFilenameMetadata("Get Lucky")

        parsed?.artist shouldBe null
        parsed?.title shouldBe "Get Lucky"
    }

    "parça numarası sanatçı sanılmaz" {
        val parsed = parseFilenameMetadata("01 - Get Lucky")

        parsed?.artist shouldBe null
        parsed?.title shouldBe "Get Lucky"
    }

    "noktalı parça numarası atılır" {
        val parsed = parseFilenameMetadata("07. Get Lucky")

        parsed?.artist shouldBe null
        parsed?.title shouldBe "Get Lucky"
    }

    "parça numarası ve sanatçı birlikte çözülür" {
        val parsed = parseFilenameMetadata("01 - Daft Punk - Get Lucky")

        parsed?.artist shouldBe "Daft Punk"
        parsed?.title shouldBe "Get Lucky"
    }

    "kısa çizgi ayırıcısı da kabul edilir" {
        val parsed = parseFilenameMetadata("Daft Punk \u2013 Get Lucky")

        parsed?.artist shouldBe "Daft Punk"
        parsed?.title shouldBe "Get Lucky"
    }

    "uzun çizgi ayırıcısı da kabul edilir" {
        val parsed = parseFilenameMetadata("Daft Punk \u2014 Get Lucky")

        parsed?.artist shouldBe "Daft Punk"
        parsed?.title shouldBe "Get Lucky"
    }

    "ilk ayırıcıdan sonrası başlığın parçasıdır" {
        val parsed = parseFilenameMetadata("Daft Punk - Get Lucky - Live")

        parsed?.artist shouldBe "Daft Punk"
        parsed?.title shouldBe "Get Lucky - Live"
    }

    "boş dosya adı çözümlenmez" {
        parseFilenameMetadata("   ") shouldBe null
    }

    "başlığı boş bırakan dosya adı çözümlenmez" {
        parseFilenameMetadata("Daft Punk - ") shouldBe null
    }

    "başlık farklıysa güncelleme gerekir" {
        needsMetadataUpdate(
            currentTitle = "Unknown",
            currentArtist = "Unknown",
            parsed = FilenameMetadata("Get Lucky", "Daft Punk")
        ) shouldBe true
    }

    "sanatçı farklıysa güncelleme gerekir" {
        needsMetadataUpdate(
            currentTitle = "Get Lucky",
            currentArtist = "Unknown",
            parsed = FilenameMetadata("Get Lucky", "Daft Punk")
        ) shouldBe true
    }

    "değerler aynıysa dosya yeniden yazılmaz" {
        needsMetadataUpdate(
            currentTitle = "Get Lucky",
            currentArtist = "Daft Punk",
            parsed = FilenameMetadata("Get Lucky", "Daft Punk")
        ) shouldBe false
    }

    "sanatçısız dosyada başlık aynıysa güncelleme gerekmez" {
        needsMetadataUpdate(
            currentTitle = "Get Lucky",
            currentArtist = "Bilinmeyen",
            parsed = FilenameMetadata("Get Lucky", null)
        ) shouldBe false
    }
})
