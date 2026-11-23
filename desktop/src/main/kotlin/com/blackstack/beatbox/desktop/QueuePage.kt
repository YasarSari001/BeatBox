package com.blackstack.beatbox.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings

/**
 * Oynatma kuyruğu sayfası.
 *
 * Kuyruk boşsa normal kitaplık sırası kullanılır; burada yalnızca kullanıcının
 * eklediği parçalar listelenir. Bir parçaya dokunmak onu hemen oynatır.
 */
@Composable
fun QueuePage(
    strings: Strings,
    queue: List<Track>,
    current: Track?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit,
    onRemove: (Track) -> Unit,
    onClear: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(strings.dt("queue"), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = strings.dt("queue_count").format(queue.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClear, enabled = queue.isNotEmpty()) {
                Icon(
                    Icons.Rounded.DeleteSweep,
                    contentDescription = strings.dt("queue_clear"),
                    tint = if (queue.isNotEmpty()) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        if (queue.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(52.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = strings.dt("queue_empty"),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            return
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            itemsIndexed(queue, key = { _, track -> track.file.absolutePath }) { index, track ->
                val isCurrent = track == current
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                        .clickable { onPlay(track) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = (index + 1).toString().padStart(2, '0'),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track.title,
                            fontSize = 13.5.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = listOfNotNull(
                                track.artist.takeIf { it.isNotBlank() },
                                formatTime(track.durationSeconds),
                            ).joinToString(" · "),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    if (isCurrent && isPlaying) {
                        Text("♪", fontSize = 16.sp, color = accent)
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(onClick = { onRemove(track) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = strings.dt("queue_remove"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }
        }
    }
}
