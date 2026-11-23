package com.blackstack.beatbox.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Etiket (ID3/Vorbis/MP4) düzenleme penceresi.
 *
 * Alanlar dosyadaki mevcut etiketlerle doldurulur. Boş bırakılan alanlar
 * kaydetme sırasında dosyadan silinir.
 */
@Composable
fun TagEditorDialog(
    strings: Strings,
    track: Track,
    onDismiss: () -> Unit,
    onSaved: (Track) -> Unit,
) {
    val initial = remember(track.file) {
        TagEditor.read(track.file) ?: EditableTags(
            title = track.title,
            artist = track.artist,
            album = track.album,
        )
    }
    var title by remember(track.file) { mutableStateOf(initial.title) }
    var artist by remember(track.file) { mutableStateOf(initial.artist) }
    var album by remember(track.file) { mutableStateOf(initial.album) }
    var genre by remember(track.file) { mutableStateOf(initial.genre) }
    var year by remember(track.file) { mutableStateOf(initial.year) }
    var trackNumber by remember(track.file) { mutableStateOf(initial.trackNumber) }
    var error by remember(track.file) { mutableStateOf<String?>(null) }
    var saving by remember(track.file) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun save() {
        if (saving) return
        saving = true
        error = null
        val edited = EditableTags(
            title = title,
            artist = artist,
            album = album,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
        )
        scope.launch {
            val result = withContext(Dispatchers.IO) { TagEditor.write(track.file, edited) }
            if (result.isSuccess) {
                val fallbackTitle = track.file.nameWithoutExtension
                onSaved(
                    track.copy(
                        title = edited.title.trim().ifBlank { fallbackTitle },
                        artist = edited.artist.trim(),
                        album = edited.album.trim(),
                    )
                )
            } else {
                saving = false
                error = strings.dt("tag_save_failed") +
                    (result.exceptionOrNull()?.message?.let { " ($it)" } ?: "")
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = {
            Column {
                Text(strings.dt("tag_editor"), fontSize = 18.sp)
                Text(
                    text = track.file.name,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TagField(strings.get("track_name"), title) { title = it }
                TagField(strings.get("artist"), artist) { artist = it }
                TagField(strings.dt("tag_album"), album) { album = it }
                TagField(strings.dt("tag_genre"), genre) { genre = it }
                TagField(strings.dt("tag_year"), year) { year = it }
                TagField(strings.dt("tag_track_number"), trackNumber) { trackNumber = it }

                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { save() }, enabled = !saving) {
                Text(strings.dt("tag_save"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) {
                Text(strings.get("cancel"))
            }
        },
    )
}

@Composable
private fun TagField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
