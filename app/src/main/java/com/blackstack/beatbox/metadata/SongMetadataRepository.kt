package com.blackstack.beatbox.metadata
import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.MediaStore
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
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
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
        val raw = if (audio.isOnline) {
            "${audio.title}|${audio.duration}"
        } else {
            audio.uri.toString()
        }
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
        if (!isEnabled(context) || audio.isOnline) return null

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
     * Dosya adından başlık ve sanatçıyı çıkarıp doğrudan ses dosyasının
     * etiketine (MP3 için ID3) yazar. Başarılıysa güncellenmiş [AudioFile]
     * kopyasını döner.
     *
     * MediaStore taramasında URI `content://` olduğu için yazma iki yolla
     * denenir:
     * 1. MediaStore'un `DATA` sütunundan gerçek dosya yolu bulunursa doğrudan
     *    dosyaya yazılır (Android 9 ve öncesinde WRITE_EXTERNAL_STORAGE,
     *    Android 11+ için "Tüm dosyalara erişim" izniyle).
     * 2. Yol yoksa/yazılamıyorsa içerik geçici bir dosyaya kopyalanır, etiket
     *    yazılır ve `openOutputStream(uri, "rwt")` ile geri kopyalanır. Bu yol
     *    yalnızca uygulamanın sahibi olduğu veya yazma izni verilmiş URI'lerde
     *    çalışır; aksi halde yazma sessizce atlanır.
     *
     * Yazma başarısızsa null döner ve mevcut [AudioFile] değerleri korunur.
     * Dosya adı biçimi: "Sanatçı - Başlık.mp3" veya sadece "Başlık.mp3".
     */
    fun fixMetadataFromFilename(
        audio: AudioFile,
        context: Context,
        knownPath: String? = null
    ): AudioFile? {
        if (audio.isOnline) return null

        val path = knownPath?.takeIf { it.isNotBlank() } ?: queryDataPath(context, audio.uri)
        val fileName = path?.let { File(it).name }
            ?: queryDisplayName(context, audio.uri)
            ?: if (audio.uri.scheme == "file") audio.uri.lastPathSegment else null
        val parsed = fileName
            ?.substringBeforeLast('.', fileName)
            ?.let(::parseFilenameMetadata)
            ?: return null

        // MediaStore zaten aynı değerleri taşıyorsa her taramada yeniden yazma.
        if (!needsMetadataUpdate(audio.title, audio.artist, parsed)) return audio

        if (!writeTags(context, audio.uri, path, parsed)) {
            Log.w("SongMetadata", "Etiket yazılamadı: ${audio.uri}")
            return null
        }

        // Bir sonraki tarama dosya adıyla aynı değerleri görsün diye MediaStore
        // satırını da güncelle. Böylece yenileme sırasında eski başlık dönmez.
        updateMediaStore(context, audio.uri, parsed)

        return audio.copy(
            title = parsed.title,
            artist = parsed.artist ?: audio.artist
        )
    }

    @Suppress("DEPRECATION")
    private fun queryDataPath(context: Context, uri: Uri): String? {
        if (uri.scheme != "content") return null
        return runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Media.DATA),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()
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

    private fun writeTags(
        context: Context,
        uri: Uri,
        knownPath: String?,
        parsed: FilenameMetadata
    ): Boolean {
        val directFile = knownPath
            ?.let(::File)
            ?.takeIf { it.isFile && it.canWrite() }
        if (directFile != null && writeTagsToFile(directFile, parsed)) return true

        if (uri.scheme != "content") return false

        // Scoped storage: yazma izni yoksa geçici kopya oluşturup boşa
        // çalışmadan önce URI'nin gerçekten yazılabilir olduğunu doğrula.
        if (!canWriteContentUri(context, uri)) {
            Log.w(
                "SongMetadata",
                "İçerik URI'si yazılamıyor (tüm dosyalara erişim gerekli): $uri"
            )
            return false
        }

        // DATA yolu yoksa veya yazılamıyorsa geçici kopya üzerinden yaz.
        val extension = knownPath
            ?.substringAfterLast('.', "")
            ?.takeIf { it.isNotBlank() }
            ?: queryDisplayName(context, uri)
                ?.substringAfterLast('.', "")
                ?.takeIf { it.isNotBlank() }
            ?: "mp3"
        val temp = runCatching {
            File.createTempFile("beatbox_tag_", ".$extension", context.cacheDir)
        }.getOrNull() ?: return false

        return try {
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
            if (!copied) return false

            if (!writeTagsToFile(temp, parsed)) return false

            runCatching {
                context.contentResolver.openOutputStream(uri, "rwt")?.use { output ->
                    temp.inputStream().use { input -> input.copyTo(output) }
                } != null
            }.getOrDefault(false)
        } catch (e: Exception) {
            Log.w("SongMetadata", "İçerik URI'sine etiket yazılamadı: $uri", e)
            false
        } finally {
            temp.delete()
        }
    }

    /**
     * İçerik URI'sinin yazılabilirliğini yan etkisiz biçimde sınar.
     * `openOutputStream` önce dosyayı kesebileceği için burada yalnızca
     * `openFileDescriptor(uri, "rw")` kullanılır.
     */
    private fun canWriteContentUri(context: Context, uri: Uri): Boolean =
        runCatching {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { true } ?: false
        }.getOrDefault(false)

    private fun writeTagsToFile(file: File, parsed: FilenameMetadata): Boolean = runCatching {
        val taggedFile = AudioFileIO.read(file)
        val tag = taggedFile.tagOrCreateAndSetDefault
        val currentTitle = runCatching { tag.getFirst(FieldKey.TITLE) }.getOrNull()
        val currentArtist = runCatching { tag.getFirst(FieldKey.ARTIST) }.getOrNull()

        if (currentTitle == parsed.title &&
            (parsed.artist == null || currentArtist == parsed.artist)
        ) {
            return@runCatching true
        }

        tag.setField(FieldKey.TITLE, parsed.title)
        parsed.artist?.let { tag.setField(FieldKey.ARTIST, it) }
        AudioFileIO.write(taggedFile)
        true
    }.getOrElse { e ->
        Log.w("SongMetadata", "Etiket dosyaya yazılamadı: ${file.name}", e)
        false
    }

    private fun updateMediaStore(context: Context, uri: Uri, parsed: FilenameMetadata) {
        if (uri.scheme != "content") return
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.TITLE, parsed.title)
            parsed.artist?.let { put(MediaStore.Audio.Media.ARTIST, it) }
        }
        runCatching {
            context.contentResolver.update(uri, values, null, null)
        }.onFailure {
            Log.w("SongMetadata", "MediaStore satırı güncellenemedi: $uri", it)
        }
    }
}
