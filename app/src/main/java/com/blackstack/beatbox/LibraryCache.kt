package com.blackstack.beatbox

import android.content.Context
import android.net.Uri
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Son taranan kütüphanenin diskteki kopyası.
 *
 * Uygulama açılışında MediaStore yeniden taranmaz; bu önbellek anında yüklenir.
 * Yeni tarama yalnızca kullanıcı aşağı çekerek yenilediğinde, klasör/süre
 * ayarları değiştiğinde veya önbellek yoksa yapılır.
 */
data class CachedLibrary(
    val songs: List<AudioFile>,
    val folders: List<String>,
    val selectedFolders: Set<String>,
    val minDurationSeconds: Int
) {
    /** Önbellek, mevcut filtre ayarlarıyla aynı taramaya ait mi? */
    fun matches(selected: Set<String>, minDuration: Int): Boolean =
        selectedFolders == selected && minDurationSeconds == minDuration
}

object LibraryCache {

    private const val FILE_NAME = "library_cache_v1.json"
    private const val VERSION = 1
    private const val TAG = "LibraryCache"

    @Synchronized
    fun load(context: Context): CachedLibrary? = runCatching {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return@runCatching null

        val root = JSONObject(file.readText())
        if (root.optInt("version") != VERSION) return@runCatching null

        val songsArray = root.optJSONArray("songs") ?: return@runCatching null
        val songs = buildList {
            for (index in 0 until songsArray.length()) {
                val item = songsArray.optJSONObject(index) ?: continue
                runCatching { item.toAudioFile() }.getOrNull()?.let(::add)
            }
        }

        CachedLibrary(
            songs = songs,
            folders = root.optJSONArray("folders").toStringList(),
            selectedFolders = root.optJSONArray("selectedFolders").toStringList().toSet(),
            minDurationSeconds = root.optInt("minDurationSeconds", 30)
        )
    }.getOrElse { error ->
        Log.w(TAG, "Kitaplık önbelleği okunamadı", error)
        null
    }

    @Synchronized
    fun save(
        context: Context,
        songs: List<AudioFile>,
        folders: List<String>,
        selectedFolders: Set<String>,
        minDurationSeconds: Int
    ) {
        runCatching {
            val root = JSONObject().apply {
                put("version", VERSION)
                put("savedAt", System.currentTimeMillis())
                put("minDurationSeconds", minDurationSeconds)
                put("folders", JSONArray(folders))
                put("selectedFolders", JSONArray(selectedFolders.toList()))
                put("songs", JSONArray().apply {
                    songs.forEach { put(it.toJson()) }
                })
            }

            val file = File(context.filesDir, FILE_NAME)
            val temp = File(context.filesDir, "$FILE_NAME.tmp")
            temp.writeText(root.toString())
            if (file.exists()) file.delete()
            if (!temp.renameTo(file)) {
                // Bazı dosya sistemlerinde rename mevcut hedefi ezemez.
                file.writeText(root.toString())
                temp.delete()
            }
        }.onFailure { error ->
            Log.w(TAG, "Kitaplık önbelleği yazılamadı", error)
        }
    }

    private fun AudioFile.toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("title", title)
        put("artist", artist)
        put("duration", duration)
        put("uri", uri.toString())
        put("albumId", albumId)
        put("folderName", folderName)
        put("dateAdded", dateAdded)
        put("isOnline", isOnline)
        putNullable("artworkUrl", artworkUrl)
        putNullable("artworkPath", artworkPath)
        put("isExplicit", isExplicit)
        putNullable("albumArtist", albumArtist)
        putNullable("albumName", albumName)
    }

    private fun JSONObject.toAudioFile(): AudioFile = AudioFile(
        id = getLong("id"),
        title = optString("title", "Unknown"),
        artist = optString("artist", "Unknown"),
        duration = optLong("duration"),
        uri = Uri.parse(getString("uri")),
        albumId = optLong("albumId"),
        folderName = optString("folderName", "Unknown"),
        dateAdded = optLong("dateAdded"),
        isOnline = optBoolean("isOnline", false),
        artworkUrl = optStringOrNull("artworkUrl"),
        artworkPath = optStringOrNull("artworkPath"),
        isExplicit = optBoolean("isExplicit", false),
        albumArtist = optStringOrNull("albumArtist"),
        albumName = optStringOrNull("albumName")
    )

    private fun JSONObject.putNullable(key: String, value: String?) {
        put(key, value ?: JSONObject.NULL)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (index in 0 until length()) {
                optString(index).takeIf { it.isNotBlank() }?.let(::add)
            }
        }
    }
}
