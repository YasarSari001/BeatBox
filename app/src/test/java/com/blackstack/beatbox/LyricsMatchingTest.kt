package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import com.blackstack.beatbox.lyrics.LRCLIBResponse
import com.blackstack.beatbox.lyrics.LrcRedResult
import com.blackstack.beatbox.lyrics.LyricsProviderSupport

/**
 * LRCLIB eşleştirme mantığını doğrular.
 *
 * Yanlış sürüm seçimi, doğru şarkıda başka bir sanatçının sözlerinin
 * gösterilmesine yol açar; bu yüzden başlık, sanatçı ve süre birlikte
 * değerlendirilir.
 */
class LyricsMatchingTest : StringSpec({

    "normalizeMatchText aksanları ve noktalama işaretlerini temizler" {
        LyricsProviderSupport.normalizeMatchText("Müslüm Gürses - İtirazım Var!") shouldBe
            "muslum gurses itirazim var"
    }

    "stripFeatAndDecorators feat eklerini kaldırır" {
        LyricsProviderSupport.stripFeatAndDecorators("Song (feat. Someone)") shouldBe "Song"
        LyricsProviderSupport.stripFeatAndDecorators("Song feat. Someone") shouldBe "Song"
    }

    "LRCLIB doğru başlık ve sanatçıyı seçer" {
        val results = listOf(
            lrcLib(id = 1, track = "Another Song", artist = "Right Artist", duration = 200.0, synced = "[00:01.00]wrong"),
            lrcLib(id = 2, track = "Right Song", artist = "Right Artist", duration = 201.0, synced = "[00:01.00]right")
        )

        val best = LyricsProviderSupport.selectBestLrcLibCandidate(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        )

        best.shouldNotBeNull().id shouldBe 2L
    }

    "LRCLIB yanlış sanatçıyı reddeder" {
        val results = listOf(
            lrcLib(id = 1, track = "Right Song", artist = "Wrong Band", duration = 200.0, synced = "[00:01.00]wrong")
        )

        LyricsProviderSupport.selectBestLrcLibCandidate(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        ).shouldBeNull()
    }

    "LRCLIB süre toleransı dışındaki sonucu reddeder" {
        val results = listOf(
            lrcLib(id = 1, track = "Right Song", artist = "Right Artist", duration = 240.0, synced = "[00:01.00]wrong")
        )

        LyricsProviderSupport.selectBestLrcLibCandidate(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        ).shouldBeNull()
    }

    "LRCLIB senkron sürümü düz sürüme tercih eder" {
        val results = listOf(
            lrcLib(id = 1, track = "Right Song", artist = "Right Artist", duration = 200.0, plain = "plain"),
            lrcLib(id = 2, track = "Right Song", artist = "Right Artist", duration = 200.0, synced = "[00:01.00]synced")
        )

        val best = LyricsProviderSupport.selectBestLrcLibCandidate(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        )

        best.shouldNotBeNull().id shouldBe 2L
    }

    "LRCLIB feat ekli başlığı eşleştirir" {
        val results = listOf(
            lrcLib(
                id = 1,
                track = "Right Song (feat. Guest)",
                artist = "Right Artist",
                duration = 200.0,
                synced = "[00:01.00]right"
            )
        )

        val best = LyricsProviderSupport.selectBestLrcLibCandidate(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        )

        best.shouldNotBeNull().id shouldBe 1L
    }

    "lrc.red doğru başlık ve sanatçıyı seçer" {
        val results = listOf(
            lrcRed(id = "a", track = "Another Song", artist = "Right Artist", duration = 200.0),
            lrcRed(id = "b", track = "Right Song", artist = "Right Artist", duration = 200.0)
        )

        val best = LyricsProviderSupport.selectLrcRedResult(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        )

        best.shouldNotBeNull().id shouldBe "b"
    }

    "lrc.red yanlış başlığı reddeder" {
        val results = listOf(
            lrcRed(id = "a", track = "Completely Different", artist = "Right Artist", duration = 200.0)
        )

        LyricsProviderSupport.selectLrcRedResult(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        ).shouldBeNull()
    }

    "lrc.red süre toleransı dışındaki sonucu reddeder" {
        val results = listOf(
            lrcRed(id = "a", track = "Right Song", artist = "Right Artist", duration = 260.0)
        )

        LyricsProviderSupport.selectLrcRedResult(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        ).shouldBeNull()
    }

    "lrc.red feat ekli başlığı eşleştirir" {
        val results = listOf(
            lrcRed(id = "a", track = "Right Song (feat. Guest)", artist = "Right Artist", duration = 200.0)
        )

        val best = LyricsProviderSupport.selectLrcRedResult(
            results = results,
            trackName = "Right Song",
            artistName = "Right Artist",
            durationMs = 200_000L
        )

        best.shouldNotBeNull().id shouldBe "a"
    }
})

private fun lrcLib(
    id: Long,
    track: String,
    artist: String,
    duration: Double,
    synced: String? = null,
    plain: String? = null
) = LRCLIBResponse(
    id = id,
    trackName = track,
    artistName = artist,
    albumName = null,
    duration = duration,
    syncedLyrics = synced,
    plainLyrics = plain
)

private fun lrcRed(
    id: String,
    track: String,
    artist: String,
    duration: Double
) = LrcRedResult(
    id = id,
    isrc = null,
    trackName = track,
    artistName = artist,
    albumName = null,
    duration = duration,
    lyricsUrl = "https://lrc.red/api/v1/lyrics/$id.ttml",
    timingType = "word"
)
