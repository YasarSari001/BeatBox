package com.blackstack.beatbox.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.desktop.lyrics.LyricsProvider
import com.blackstack.beatbox.desktop.lyrics.LyricsRequest
import com.blackstack.beatbox.desktop.lyrics.LyricsState
import com.blackstack.beatbox.desktop.lyrics.SyncedLyrics
import com.blackstack.beatbox.desktop.lyrics.SyncedLyricsLine

/**
 * Şarkı sözleri sayfası (APK'daki karaoke ekranının masaüstü karşılığı).
 *
 * - Satırlar oynatma konumuna göre otomatik kaydırılır.
 * - Geçerli satır vurgulanır; kelime zamanları varsa kelimeler sırayla renklenir.
 * - Bir satıra tıklamak o ana atlar.
 * - Gecikme (offset) ±0,5 sn adımlarla ayarlanabilir.
 */
@Composable
fun LyricsPage(
    strings: Strings,
    track: Track?,
    positionSeconds: Double,
    onSeek: (Double) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    var refreshToken by remember { mutableStateOf(0) }
    var state by remember(track?.file) {
        mutableStateOf<LyricsState>(if (track == null) LyricsState.Idle else LyricsState.Loading)
    }
    var offsetMs by remember(track?.file) { mutableStateOf(0L) }

    LaunchedEffect(track?.file, refreshToken) {
        val current = track ?: return@LaunchedEffect
        state = LyricsState.Loading
        state = LyricsProvider.load(
            LyricsRequest(
                title = current.title,
                artist = current.artist,
                album = current.album,
                durationSeconds = current.durationSeconds,
                file = current.file,
            ),
            refresh = refreshToken > 0,
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(
                Icons.Rounded.Lyrics,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = strings.dt("lyrics"),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = track?.let { "${it.title} — ${it.artist.ifBlank { strings.get("artist") }}" }
                        ?: strings.dt("nothing_playing"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            val lyrics = (state as? LyricsState.Success)?.lyrics
            if (lyrics != null) {
                if (lyrics.synced) {
                    OffsetButton("−0.5s") { offsetMs -= 500 }
                    Text(
                        text = offsetLabel(offsetMs),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    OffsetButton("+0.5s") { offsetMs += 500 }
                    Spacer(Modifier.width(10.dp))
                }
                SourceChip(lyrics.metadata.source ?: strings.dt("lyrics_source"))
                Spacer(Modifier.width(8.dp))
            }

            IconButton(onClick = { refreshToken++ }, enabled = track != null) {
                Icon(
                    Icons.Rounded.Refresh,
                    contentDescription = strings.dt("lyrics_refresh"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        when (val snapshot = state) {
            LyricsState.Idle -> CenterMessage(strings.dt("lyrics_find_hint"), null)
            LyricsState.Loading -> CenterMessage(strings.dt("lyrics_loading")) {
                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            is LyricsState.Error -> CenterMessage(
                message = strings.dt(snapshot.message),
                trailing = {
                    Icon(
                        Icons.Rounded.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(34.dp),
                    )
                },
            )
            is LyricsState.Success -> LyricsBody(
                lyrics = snapshot.lyrics,
                positionMs = (positionSeconds * 1000).toLong() + offsetMs,
                onSeek = onSeek,
                unsyncedLabel = strings.dt("lyrics_unsynced"),
            )
        }
    }
}

@Composable
private fun LyricsBody(
    lyrics: SyncedLyrics,
    positionMs: Long,
    onSeek: (Double) -> Unit,
    unsyncedLabel: String,
) {
    val listState = rememberLazyListState()
    val accent = MaterialTheme.colorScheme.primary
    val normal = MaterialTheme.colorScheme.onSurfaceVariant
    val currentIndex = remember(lyrics, positionMs) {
        if (lyrics.synced) currentLineIndex(lyrics.lines, positionMs) else -1
    }

    LaunchedEffect(currentIndex, lyrics) {
        if (lyrics.synced && currentIndex >= 0) {
            listState.animateScrollToItem((currentIndex - 2).coerceAtLeast(0))
        }
    }

    Column(Modifier.fillMaxSize()) {
        if (!lyrics.synced) {
            Text(
                text = unsyncedLabel,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(lyrics.lines) { index, line ->
                val isCurrent = index == currentIndex
                val baseColor = when {
                    !lyrics.synced -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    isCurrent -> accent
                    line.timestampMs <= positionMs -> normal.copy(alpha = 0.75f)
                    else -> normal.copy(alpha = 0.45f)
                }

                val content: AnnotatedString = if (isCurrent && line.words.isNotEmpty()) {
                    buildKaraokeLine(line, positionMs, accent, normal)
                } else {
                    AnnotatedString(line.text)
                }

                Text(
                    text = content,
                    fontSize = if (isCurrent) 21.sp else 17.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = baseColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = lyrics.synced) {
                            onSeek(line.timestampMs / 1000.0)
                        }
                        .padding(vertical = 7.dp, horizontal = 4.dp),
                )
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/** Geçerli satırın kelimelerini söylenme sırasına göre renklendirir. */
private fun buildKaraokeLine(
    line: SyncedLyricsLine,
    positionMs: Long,
    accent: Color,
    idle: Color,
): AnnotatedString = buildAnnotatedString {
    line.words.forEach { word ->
        val spoken = word.startMs <= positionMs
        withStyle(SpanStyle(color = if (spoken) accent else idle.copy(alpha = 0.55f))) {
            append(word.text)
        }
    }
}

/** İkili arama ile o anki satırı bulur; hiçbiri başlamadıysa -1 döner. */
private fun currentLineIndex(lines: List<SyncedLyricsLine>, positionMs: Long): Int {
    if (lines.isEmpty()) return -1
    var low = 0
    var high = lines.lastIndex
    var result = -1
    while (low <= high) {
        val mid = (low + high) / 2
        if (lines[mid].timestampMs <= positionMs) {
            result = mid
            low = mid + 1
        } else {
            high = mid - 1
        }
    }
    return result
}

@Composable
private fun OffsetButton(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = Modifier.clickable { onClick() },
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun SourceChip(source: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        modifier = Modifier.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
            shape = RoundedCornerShape(10.dp),
        ),
    ) {
        Text(
            text = source,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CenterMessage(message: String, trailing: (@Composable () -> Unit)? = null) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            trailing?.invoke()
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(460.dp),
            )
        }
    }
}

private fun offsetLabel(offsetMs: Long): String {
    val seconds = offsetMs / 1000.0
    val sign = if (seconds >= 0) "+" else "−"
    return "%.1fs".format(kotlin.math.abs(seconds)).let { "$sign$it" }
}
