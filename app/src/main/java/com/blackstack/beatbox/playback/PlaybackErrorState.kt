package com.blackstack.beatbox.playback

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Oynatılamayan bir şarkının son hatası. */
data class PlaybackFailure(
    val audioId: Long,
    val message: String
)

/**
 * Oynatma hatasını görünüm katmanına taşır.
 *
 * Hata şarkının kapağı üzerinde gösterilir; şarkı listesine, etiketlere veya
 * dosyaya yazılmaz. Yeni bir parça çalınmaya başladığında [clear] ile silinir.
 */
object PlaybackErrorState {
    var latest by mutableStateOf<PlaybackFailure?>(null)
        private set

    fun report(audioId: Long, message: String) {
        val text = message.trim().ifBlank { "unknown" }
        latest = PlaybackFailure(audioId, text)
    }

    /** [audioId] şarkısı için hata varsa döner; başka şarkının hatası gösterilmez. */
    fun failureFor(audioId: Long): PlaybackFailure? = latest?.takeIf { it.audioId == audioId }

    fun clear() {
        latest = null
    }
}
