package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.io.EOFException
import java.io.IOException
import com.blackstack.beatbox.playback.causeChain

/**
 * Unit tests for the playback error helpers.
 */
class PlaybackErrorsTest : StringSpec({

    "causeChain keeps nested failures" {
        val rootCause = EOFException("stream ended")
        val sourceCause = IOException("read failed", rootCause)
        val error = IllegalStateException("playback failed", sourceCause)

        error.causeChain() shouldBe listOf(error, sourceCause, rootCause)
    }

    "causeChain of an error without cause is a single element" {
        val error = IllegalStateException("playback failed")

        error.causeChain() shouldBe listOf(error)
    }
})
