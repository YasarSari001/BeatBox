package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import com.blackstack.beatbox.lyrics.TTMLParser

/**
 * TTML ayrıştırıcısının satır ve kelime zamanlamalarını doğrular.
 *
 * BetterLyrics, lrc.red ve Unison aynı TTML ayrıştırıcısını kullandığı için
 * buradaki hatalar tüm bu sağlayıcıları etkiler.
 */
class TTMLParserTest : StringSpec({

    "satır düzeyinde TTML zaman damgalarını okur" {
        val ttml = """
            <tt><body><div>
              <p begin="00:00:12.345">First line</p>
              <p begin="12.5s">Second line</p>
            </div></body></tt>
        """.trimIndent()

        val nodes = TTMLParser.parseTTML(ttml)

        nodes.size shouldBe 2
        nodes[0].timestampMs shouldBe 12_345L
        nodes[0].text shouldBe "First line"
        nodes[1].timestampMs shouldBe 12_500L
        nodes[1].text shouldBe "Second line"
    }

    "kelime düzeyindeki span etiketlerini gelişmiş LRC'ye çevirir" {
        val ttml = """
            <tt><body><div>
              <p begin="00:00:12.000" end="00:00:14.000"><span begin="00:00:12.000">Hello </span><span begin="00:00:12.500">world</span></p>
            </div></body></tt>
        """.trimIndent()

        val node = TTMLParser.parseTTML(ttml).single()

        node.text shouldBe "Hello world"
        node.words.size shouldBe 2
        node.words[0].text shouldBe "Hello "
        node.words[1].text shouldBe "world"

        TTMLParser.toLRC(listOf(node)).trim() shouldBe
            "[00:12.00]<00:12.00>Hello <00:12.50>world"
    }

    "çeviri span'ları satırdan çıkarılır" {
        val ttml = """
            <tt><body><div>
              <p begin="00:00:12.000"><span begin="00:00:12.000">Hello </span><span begin="00:00:12.500">world</span><span ttm:role="x-translation" begin="00:00:12.000">Merhaba dünya</span></p>
            </div></body></tt>
        """.trimIndent()

        val node = TTMLParser.parseTTML(ttml).single()

        node.text shouldBe "Hello world"
        node.words.size shouldBe 2
    }

    "HTML varlıkları çözülür" {
        val ttml = """<tt><body><p begin="1.0">Rock &amp; Roll &#39;n&#39; roll</p></body></tt>"""

        val node = TTMLParser.parseTTML(ttml).single()

        node.text shouldBe "Rock & Roll 'n' roll"
    }

    "iç içe span'larda yalnızca yaprak zamanlamalar kullanılır" {
        val ttml = """
            <tt><body><div>
              <p begin="00:00:01.000"><span begin="00:00:01.000"><span begin="00:00:01.000">Hello </span><span begin="00:00:01.500">world</span></span></p>
            </div></body></tt>
        """.trimIndent()

        val node = TTMLParser.parseTTML(ttml).single()

        node.text shouldBe "Hello world"
        node.words.size shouldBe 2
    }

    "span dışında kalan metin varsa kelime hizalaması güvenli biçimde bırakılır" {
        val ttml = """
            <tt><body><div>
              <p begin="00:00:01.000">Intro <span begin="00:00:01.000">Hello </span><span begin="00:00:01.500">world</span></p>
            </div></body></tt>
        """.trimIndent()

        val node = TTMLParser.parseTTML(ttml).single()

        node.text shouldBe "Intro Hello world"
        node.words shouldBe emptyList()
    }

    "boş TTML belgesi boş liste döner" {
        TTMLParser.parseTTML("<tt><head><metadata/></head><body><div></div></body></tt>") shouldBe emptyList()
    }
})
