package com.blackstack.beatbox.library
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.metadata.SongMetadataRepository
import com.blackstack.beatbox.ui.components.loadAudioFilesAndFolders

/**
 * Kitaplık listesinin durumu.
 *
 * Uygulama açılışında MediaStore taranmaz; son tarama [LibraryCache]'ten
 * anında yüklenir. Tam tarama yalnızca kullanıcı aşağı çekerek yenilediğinde,
 * klasör/süre ayarları değiştiğinde veya önbellek yoksa yapılır.
 *
 * Tarama hızlıdır (yalnızca MediaStore sorgusu). Dosya adından okunan başlık ve
 * sanatçı yalnızca görünümde, arka planda ayrı bir geçişte uygulanır; ses
 * dosyasının etiketi ve MediaStore satırı hiçbir zaman değiştirilmez.
 */
@Stable
class LibraryState(
    private val context: Context,
    private val scope: CoroutineScope
) {
    var songs by mutableStateOf<List<AudioFile>>(emptyList())
        private set
    var folders by mutableStateOf<List<String>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isRefreshing by mutableStateOf(false)
        private set

    private var initialized = false
    private var permissionGranted = false
    private var selectedFolders: Set<String> = emptySet()
    private var minDurationSeconds = 30
    private var metadataJob: Job? = null
    private val removedIds = java.util.Collections.synchronizedSet(mutableSetOf<Long>())

    /**
     * İlk açılışta önbelleği yükler; ayarlar değişince yeni tarama başlatır.
     * [hasPermission] false ise yalnızca yükleme durumunu kapatır.
     */
    suspend fun initialize(
        hasPermission: Boolean,
        selected: Set<String>,
        minDuration: Int
    ) {
        permissionGranted = hasPermission
        selectedFolders = selected
        minDurationSeconds = minDuration

        if (!hasPermission) {
            isLoading = false
            return
        }

        if (!initialized) {
            initialized = true
            val cached = withContext(Dispatchers.IO) {
                LibraryCache.load(context)
                    ?.takeIf { it.songs.isNotEmpty() && it.matches(selected, minDuration) }
                    ?.let { it.copy(songs = SongMetadataRepository.applyCached(context, it.songs)) }
            }
            if (cached != null) {
                songs = cached.songs
                folders = cached.folders
                isLoading = false
                enrichUnknownArtists()
                return
            }
        }

        refresh()
    }

    /** Ayar değişikliğinden sonra yeni filtrelerle tam tarama. */
    fun refresh(selected: Set<String>, minDuration: Int) {
        selectedFolders = selected
        minDurationSeconds = minDuration
        refresh()
    }

    /** Aşağı çekerek yenileme veya ayar değişikliğiyle tam tarama. */
    fun refresh() {
        if (!permissionGranted || isRefreshing) return
        isLoading = true
        isRefreshing = true
        removedIds.clear()

        scope.launch {
            try {
                val selected = selectedFolders
                val minDuration = minDurationSeconds
                val (loaded, scannedFolders) = withContext(Dispatchers.IO) {
                    loadAudioFilesAndFolders(context, minDuration, selected)
                }
                val enriched = SongMetadataRepository.applyCached(context, loaded)

                songs = enriched
                folders = scannedFolders
                isLoading = false
                isRefreshing = false

                // Hızlı tarama sonucu hemen önbelleğe yazılır; sonraki açılış
                // MediaStore'u hiç taramaz.
                withContext(Dispatchers.IO) {
                    LibraryCache.save(context, enriched, scannedFolders, selected, minDuration)
                }

                startDisplayMetadataPass(enriched, scannedFolders, selected, minDuration)
                enrichUnknownArtists()
            } finally {
                isRefreshing = false
            }
        }
    }

    /** Kullanıcı şarkı sildiğinde listeyi ve önbelleği günceller. */
    fun removeSong(id: Long) {
        // Arka plandaki etiket düzeltmesi silinen şarkıyı önbelleğe geri
        // yazmasın diye durdurulur ve kimliği işaretlenir.
        removedIds.add(id)
        metadataJob?.cancel()
        val remaining = songs.filter { it.id != id }
        songs = remaining
        scope.launch(Dispatchers.IO) {
            LibraryCache.save(context, remaining, folders, selectedFolders, minDurationSeconds)
        }
    }

    private fun enrichUnknownArtists() {
        scope.launch {
            SongMetadataRepository.enrichUnknownArtists(context, songs) { id, artist, artworkPath ->
                songs = SongMetadataRepository.applyResolved(songs, id, artist, artworkPath)
            }
        }
    }

    private fun startDisplayMetadataPass(
        scanned: List<AudioFile>,
        foldersSnapshot: List<String>,
        selectedSnapshot: Set<String>,
        minDurationSnapshot: Int
    ) {
        metadataJob?.cancel()
        metadataJob = scope.launch(Dispatchers.IO) {
            val updatedById = mutableMapOf<Long, AudioFile>()
            val batch = mutableListOf<AudioFile>()

            for (song in scanned) {
                if (!isActive) break
                // Yalnızca görünümü düzeltir; dosyaya yazmaz.
                val display = SongMetadataRepository.displayMetadataFor(song, context)
                if (display != null) {
                    updatedById[song.id] = display
                    batch += display
                }
                if (batch.size >= 25) {
                    applyBatch(batch)
                    batch.clear()
                }
            }
            if (batch.isNotEmpty()) applyBatch(batch)

            val finalList = scanned
                .map { updatedById[it.id] ?: it }
                .filterNot { it.id in removedIds }
            LibraryCache.save(
                context,
                finalList,
                foldersSnapshot,
                selectedSnapshot,
                minDurationSnapshot
            )
        }
    }

    private suspend fun applyBatch(batch: List<AudioFile>) {
        withContext(Dispatchers.Main) {
            val byId = batch.associateBy { it.id }
            songs = songs.map { byId[it.id] ?: it }
        }
    }
}

@Composable
fun rememberLibraryState(context: Context): LibraryState {
    val scope = rememberCoroutineScope()
    return remember(context) {
        LibraryState(context.applicationContext, scope)
    }
}
