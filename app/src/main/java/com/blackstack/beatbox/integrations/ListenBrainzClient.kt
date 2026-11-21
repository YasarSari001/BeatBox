/*
 * BeatBox Project (C) 2026
 *
 * Metrolist'in ListenBrainz scrobbling ayarlarından (GPL-3.0) esinlenmiştir.
 * ListenBrainz açık API'si kullanıcı token'ı ile çalışır; uygulama anahtarı
 * gerekmez.
 */

package com.blackstack.beatbox.integrations

import android.content.SharedPreferences
import androidx.core.content.edit
import com.blackstack.beatbox.AudioFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Ayarlar ekranındaki ListenBrainz tercihleri. */
data class ListenBrainzSettings(
    val token: String,
    val enabled: Boolean,
    val nowPlaying: Boolean,
) {
    companion object {
        const val PREFS_NAME = "BeatBoxSettings"
        private const val KEY_TOKEN = "listenbrainz_token"
        private const val KEY_ENABLED = "listenbrainz_enabled"
        private const val KEY_NOW_PLAYING = "listenbrainz_now_playing"

        fun load(prefs: SharedPreferences): ListenBrainzSettings = ListenBrainzSettings(
            token = prefs.getString(KEY_TOKEN, "").orEmpty().trim(),
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            nowPlaying = prefs.getBoolean(KEY_NOW_PLAYING, true),
        )

        fun saveToken(prefs: SharedPreferences, token: String) {
            prefs.edit { putString(KEY_TOKEN, token.trim()) }
        }

        fun saveEnabled(prefs: SharedPreferences, enabled: Boolean) {
            prefs.edit { putBoolean(KEY_ENABLED, enabled) }
        }

        fun saveNowPlaying(prefs: SharedPreferences, enabled: Boolean) {
            prefs.edit { putBoolean(KEY_NOW_PLAYING, enabled) }
        }
    }
}

/**
 * ListenBrainz "submit-listens" API'sinin küçük istemcisi.
 *
 * Scrobbling yalnızca kullanıcı token'ı ve etkin ayar varsa çalışır; hata
 * durumları çalma akışını etkilemez, sessizce Result.failure döner.
 */
object ListenBrainzClient {
    private const val BASE_URL = "https://api.listenbrainz.org/1/"
    private const val MIN_SCROBBLE_DURATION_MS = 30_000L
    private const val MAX_SCROBBLE_DELAY_MS = 240_000L

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .callTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    /** Scrobble için gereken dinleme eşiği: parçanın yarısı, en fazla 4 dakika. */
    fun scrobbleThresholdMs(durationMs: Long): Long {
        if (durationMs < MIN_SCROBBLE_DURATION_MS) return Long.MAX_VALUE
        return minOf(durationMs / 2, MAX_SCROBBLE_DELAY_MS)
    }

    /** Token'ı doğrular ve kullanıcı adını döndürür. */
    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(BASE_URL + "validate-token")
                .header("Authorization", "Token $token")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("HTTP ${response.code}")
                }
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                // ListenBrainz geçersiz token'da da HTTP 200 döner; asıl
                // gösterge gövdedeki "valid" alanıdır.
                if (json.has("valid") && !json.optBoolean("valid", false)) {
                    error(json.optString("message").ifBlank { "Invalid token" })
                }
                json.optString("user_name").ifBlank {
                    json.optString("userName").ifBlank { "ListenBrainz" }
                }
            }
        }
    }

    suspend fun submitNowPlaying(token: String, audio: AudioFile): Result<Unit> =
        submit(token, "playing_now", audio, listenedAt = null)

    suspend fun submitListen(token: String, audio: AudioFile, listenedAt: Long): Result<Unit> =
        submit(token, "single", audio, listenedAt)

    private suspend fun submit(
        token: String,
        listenType: String,
        audio: AudioFile,
        listenedAt: Long?,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val trackMetadata = JSONObject().apply {
                put("artist_name", audio.artist.ifBlank { "Unknown" })
                put("track_name", audio.title)
                audio.albumName?.takeIf { it.isNotBlank() }?.let { put("release_name", it) }
                put(
                    "additional_info",
                    JSONObject().apply {
                        if (audio.duration > 0) put("duration_ms", audio.duration)
                        put("media_player", "BeatBox")
                        put("submission_client", "BeatBox")
                    }
                )
            }
            val listen = JSONObject().apply {
                put("track_metadata", trackMetadata)
                if (listenedAt != null) put("listened_at", listenedAt)
            }
            val payload = JSONObject().apply {
                put("listen_type", listenType)
                put("payload", org.json.JSONArray().put(listen))
            }

            val request = Request.Builder()
                .url(BASE_URL + "submit-listens")
                .header("Authorization", "Token $token")
                .post(payload.toString().toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("HTTP ${response.code}")
            }
        }
    }
}
