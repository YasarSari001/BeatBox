package com.blackstack.beatbox.metadata
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.content.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.Collections
import com.blackstack.beatbox.AudioFile

/**
 * Yerel şarkıların eksik sanatçı adını ve albüm kapağını çevrim içi kaynaklardan
 * tamamlar; sonuçları diske ve SharedPreferences'a önbelleğe alır.
 *
 * Eşleşme bulunamazsa [AudioFile] üzerindeki mevcut değerler değiştirilmez.
 */
object SongMetadataRepository {

    private const val PREFS_NAME = "song_metadata_cache_v1"
    private const val SETTINGS_PREFS_NAME = "BeatBoxSettings"
    private const val PREF_AUTO = "auto_song_metadata"
    private const val MISS_TTL_MS = 3L * 24 * 60 * 60 * 1000

    /**
     * Kapağı çözülen şarkılar. [AlbumArtImage] bu haritayı izleyerek kapağı
     * ağ isteği bittikten sonra otomatik olarak gösterir.
     */
    val resolvedArtwork = mutableStateMapOf<Long, String>()

    private val artistSemaphore = Semaphore(2)
    private val inFlightArtwork = mutableMapOf<String, Deferred<String?>>()
    private val inFlightLock = Mutex()
    private val artistInFlight = Collections.synchronizedSet(mutableSetOf<String>())
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Ayar anahtarı uygulamanın genel tercihlerinde tutulur; önbellek ayrıdır. */
    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(SETTINGS_PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(PREF_AUTO, true)

    private fun preferences(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * MediaStore kimliği değişse bile aynı dosyayı eşleştirmek için URI tabanlı
     * sabit bir anahtar üretir.
     */
    fun cacheKey(audio: AudioFile): String {
        val raw = audio.uri.toString()
        return MessageDigest.getInstance("SHA-1")
            .digest(raw.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    /**
     * Daha önce çözülen sanatçı ve kapakları listedeki şarkılara uygular.
     * Ana iş parçacığından çağrılmalıdır.
     */
    fun applyCached(context: Context, songs: List<AudioFile>): List<AudioFile> {
        // Otomatik tamamlama kapatılsa bile daha önce çözülen bilgiler korunur;
        // anahtar yalnızca yeni ağ aramalarını durdurur.
        val prefs = preferences(context)
        return songs.map { song ->
            val key = cacheKey(song)
            val cachedArtist = prefs.getString("artist_$key", null)
            val cachedArt = prefs.getString("art_$key", null)
                ?.takeIf { it.isNotBlank() && File(it).exists() }

            var updated = song
            if (SongMetadataMatcher.isUnknownArtist(song.artist) && !cachedArtist.isNullOrBlank()) {
                updated = updated.copy(artist = cachedArtist)
            }
            if (cachedArt != null) {
                updated = updated.copy(artworkPath = cachedArt)
                resolvedArtwork[song.id] = cachedArt
            }
            updated
        }
    }

    /**
     * Çözülen sanatçı ve kapağı yalnızca eksik alanlara uygular. Mevcut bir
     * sanatçı adı veya yerel kapak asla ezilmez.
     */
    fun applyResolved(
        songs: List<AudioFile>,
        id: Long,
        artist: String,
        artworkPath: String?
    ): List<AudioFile> = songs.map { song ->
        if (song.id != id) {
            song
        } else {
            song.copy(
                artist = if (SongMetadataMatcher.isUnknownArtist(song.artist)) artist else song.artist,
                artworkPath = song.artworkPath ?: artworkPath
            )
        }
    }

    /**
     * Sanatçısı bilinmeyen şarkılar için çevrim içi arama yapar. Bulunan
     * sanatçı ve kapak [onResolved] ile ana iş parçacığında bildirilir.
     */
    suspend fun enrichUnknownArtists(
        context: Context,
        songs: List<AudioFile>,
        onResolved: suspend (id: Long, artist: String, artworkPath: String?) -> Unit
    ) {
        if (!isEnabled(context)) return
        val prefs = preferences(context)
        val now = System.currentTimeMillis()

        val targets = songs.filter { song ->
            if (!SongMetadataMatcher.isUnknownArtist(song.artist)) return@filter false
            val key = cacheKey(song)
            if (!prefs.getString("artist_$key", null).isNullOrBlank()) return@filter false
            now - prefs.getLong("artist_miss_$key", 0L) > MISS_TTL_MS
        }
        if (targets.isEmpty()) return

        coroutineScope {
            targets.forEach { song ->
                val key = cacheKey(song)
                if (!artistInFlight.add(key)) return@forEach

                launch(Dispatchers.IO) {
                    try {
                        artistSemaphore.withPermit {
                            // Deezer'ın hız sınırına nazik davran.
                            delay(120)
                            val result = SongMetadataProvider.search(song.title, null)
                            val candidates = result.getOrNull()
                            if (candidates == null) {
                                Log.w("SongMetadata", "Sanatçı araması başarısız: ${song.title}")
                                return@withPermit
                            }

                            val match = SongMetadataMatcher.pickBest(
                                queryTitle = song.title,
                                queryArtist = null,
                                queryDurationMs = song.duration,
                                candidates = candidates
                            )
                            if (match == null) {
                                prefs.edit { putLong("artist_miss_$key", System.currentTimeMillis()) }
                                return@withPermit
                            }

                            prefs.edit { putString("artist_$key", match.artist) }
                            val artworkPath = resolveArtworkForMatch(
                                context = context,
                                audio = song,
                                key = key,
                                match = match,
                                prefs = prefs
                            )
                            withContext(Dispatchers.Main) {
                                onResolved(song.id, match.artist, artworkPath)
                            }
                        }
                    } finally {
                        artistInFlight.remove(key)
                    }
                }
            }
        }
    }

    /**
     * [AlbumArtImage] yerel kapak bulamadığında çağırır. Sonuç önbellekten
     * gelirse hemen, değilse tek bir arka plan isteğiyle döner.
     */
    suspend fun ensureArtwork(context: Context, audio: AudioFile): String? {
        if (!isEnabled(context)) return null

        audio.artworkPath?.let { path ->
            if (File(path).exists()) return path
        }

        val key = cacheKey(audio)
        val prefs = preferences(context)
        prefs.getString("art_$key", null)?.let { path ->
            if (File(path).exists()) {
                withContext(Dispatchers.Main) { resolvedArtwork[audio.id] = path }
                return path
            }
        }

        if (System.currentTimeMillis() - prefs.getLong("art_miss_$key", 0L) <= MISS_TTL_MS) {
            return null
        }

        val deferred = inFlightLock.withLock {
            inFlightArtwork[key] ?: backgroundScope.async {
                try {
                    fetchArtwork(context, audio, key, prefs)
                } finally {
                    inFlightLock.withLock { inFlightArtwork.remove(key) }
                }
            }.also { inFlightArtwork[key] = it }
        }
        return deferred.await()
    }

    private suspend fun resolveArtworkForMatch(
        context: Context,
        audio: AudioFile,
        key: String,
        match: SongMetadataCandidate,
        prefs: SharedPreferences
    ): String? {
        audio.artworkPath?.let { path ->
            if (File(path).exists()) return path
        }
        prefs.getString("art_$key", null)?.let { path ->
            if (File(path).exists()) return path
        }

        val url = match.artworkUrl
        if (url.isNullOrBlank()) {
            prefs.edit { putLong("art_miss_$key", System.currentTimeMillis()) }
            return null
        }

        val path = SongMetadataProvider.downloadArtwork(context, url, key)
        if (path != null) {
            prefs.edit { putString("art_$key", path) }
            withContext(Dispatchers.Main) { resolvedArtwork[audio.id] = path }
        }
        return path
    }

    private suspend fun fetchArtwork(
        context: Context,
        audio: AudioFile,
        key: String,
        prefs: SharedPreferences
    ): String? {
        val result = SongMetadataProvider.search(audio.title, audio.artist)
        val candidates = result.getOrNull()
        if (candidates == null) {
            Log.w("SongMetadata", "Kapak araması başarısız: ${audio.title}")
            return null
        }

        val match = SongMetadataMatcher.pickBest(
            queryTitle = audio.title,
            queryArtist = audio.artist,
            queryDurationMs = audio.duration,
            candidates = candidates
        )
        val url = match?.artworkUrl
        if (match == null || url.isNullOrBlank()) {
            prefs.edit { putLong("art_miss_$key", System.currentTimeMillis()) }
            return null
        }

        val path = SongMetadataProvider.downloadArtwork(context, url, key)
        if (path != null) {
            prefs.edit { putString("art_$key", path) }
            withContext(Dispatchers.Main) { resolvedArtwork[audio.id] = path }
        }
        return path
    }

    /**
     * Dosya adından okunan başlık ve sanatçıyı yalnızca uygulama görünümü için
     * uygular. Ses dosyasının etiketi ve MediaStore satırı değiştirilmez;
     * dosyalar salt okunur kalır. Uygulamanın diske yazdığı tek çıktı, indirilen
     * albüm kapağının önbelleğidir.
     *
     * Dosya adı biçimi: "Sanatçı - Başlık.mp3" veya sadece "Başlık.mp3".
     * Görünen değerler zaten aynıysa null döner.
     */
    fun displayMetadataFor(audio: AudioFile, context: Context): AudioFile? {
        val fileName = queryDisplayName(context, audio.uri)
            ?: if (audio.uri.scheme == "file") audio.uri.lastPathSegment else null
        val parsed = fileName
            ?.substringBeforeLast('.', fileName)
            ?.let(::parseFilenameMetadata)

        val title = cleanDisplayTitle(parsed?.title ?: audio.title)
        val artist = parsed?.artist ?: audio.artist
        if (title == audio.title && artist == audio.artist) return null

        return audio.copy(title = title, artist = artist)
    }

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return null
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
    }
}
