package com.blackstack.beatbox

import android.net.Uri
import androidx.compose.ui.graphics.Color
import java.util.UUID

enum class AuthProvider(val label: String) {
    EMAIL("E-posta"),
    GOOGLE("Google"),
    PHONE("Telefon"),
    GUEST("Misafir")
}

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val displayName: String = "BeatBox Dinleyicisi",
    val email: String = "",
    val phoneNumber: String = "",
    val bio: String = "Müzik ruhun gıdasıdır ✨",
    val avatarId: Int = 0,
    val joinedDate: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false,
    val authProvider: AuthProvider = AuthProvider.GUEST,
    val isPremium: Boolean = true
)

data class Artist(
    val name: String,
    val id: String? = null
)

@androidx.compose.runtime.Immutable
data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Long,
    val uri: Uri,
    val albumId: Long,
    val folderName: String,
    val dateAdded: Long,
    /** Çevrim içi eşleşmeden indirilen yerel kapak dosyası. */
    val artworkPath: String? = null,
    val isExplicit: Boolean = false,
    val albumArtist: String? = null,
    val albumName: String? = null
)

fun String.parseTimeMs(): Long {
    val parts = this.split(":").mapNotNull { it.trim().toLongOrNull() }
    return when (parts.size) {
        2 -> (parts[0] * 60 + parts[1]) * 1000L
        3 -> (parts[0] * 3600 + parts[1] * 60 + parts[2]) * 1000L
        else -> this.toLongOrNull() ?: 0L
    }
}

data class Playlist(
    val name: String,
    val id: String = java.util.UUID.randomUUID().toString(),
    val songIds: MutableList<Long> = mutableListOf(),
    val isSmart: Boolean = false,
    val isArtistPlaylist: Boolean = false,
    val artistName: String? = null
)

fun groupSongsByArtist(songs: List<AudioFile>, unknownLabel: String = "Unknown Artist"): Map<String, List<AudioFile>> {
    return songs.groupBy { song ->
        val artist = song.artist.trim()
        if (artist.isBlank() || artist.equals("<unknown>", ignoreCase = true)) {
            unknownLabel
        } else {
            artist
        }
    }
}


enum class SortOption { DATE, NAME }
