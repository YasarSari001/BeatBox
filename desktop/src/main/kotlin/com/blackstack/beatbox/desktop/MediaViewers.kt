package com.blackstack.beatbox.desktop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NavigateBefore
import androidx.compose.material.icons.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import kotlin.math.roundToInt

/*
 * Fotoğraf görüntüleyici ve video oynatıcı katmanları.
 *
 * Fotoğraflar ImageIO ile ölçeklenerek okunur (tam boy çözme belleği
 * şişiriyordu). Video, [VideoEngine]'in FFmpeg hattından gelen karelerle
 * çizilir; ses aynı akıştan geldiği için senkron kalır.
 */

/** Ölçekli görsel okuma; küçük resimler ve tam ekran için ortak yol. */
fun loadScaledImage(file: File, maxEdge: Int): ImageBitmap? = runCatching {
    val stream = ImageIO.createImageInputStream(file) ?: return@runCatching null
    stream.use {
        val readers: Iterator<ImageReader> = ImageIO.getImageReaders(it)
        if (!readers.hasNext()) return@runCatching null
        val reader = readers.next()
        try {
            reader.input = it
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            val step = (maxOf(width, height).toFloat() / maxEdge).roundToInt().coerceAtLeast(1)
            val param = reader.defaultReadParam
            param.setSourceSubsampling(step, step, 0, 0)
            val image: BufferedImage = reader.read(0, param)
            image.toComposeImageBitmap()
        } finally {
            reader.dispose()
        }
    }
}.getOrNull()

/** Fotoğraf küçük resimleri; aynı dosya yeniden çözülmesin. */
object ThumbnailCache {
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private const val limit = 320

    fun get(file: File): ImageBitmap? {
        val key = file.absolutePath
        cache[key]?.let { return it }
        val loaded = loadScaledImage(file, 320) ?: return null
        cache[key] = loaded
        if (cache.size > limit) {
            val oldest = cache.keys.firstOrNull() ?: return loaded
            cache.remove(oldest)
        }
        return loaded
    }
}

@Composable
fun PhotoViewerOverlay(
    photo: Track,
    photos: List<Track>,
    onClose: () -> Unit,
    onNavigate: (Track) -> Unit,
) {
    var bitmap by remember(photo.file) { mutableStateOf<ImageBitmap?>(null) }
    var loading by remember(photo.file) { mutableStateOf(true) }
    var zoom by remember(photo.file) { mutableStateOf(1f) }

    LaunchedEffect(photo.file) {
        loading = true
        zoom = 1f
        bitmap = withContext(Dispatchers.IO) { loadScaledImage(photo.file, 2600) }
        loading = false
    }

    val index = photos.indexOf(photo)
    val previous = if (index > 0) photos[index - 1] else null
    val next = if (index >= 0 && index < photos.lastIndex) photos[index + 1] else null

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xFF05060A))
            .pointerInput(photo.file) {
                detectTapGestures(onDoubleTap = { zoom = if (zoom > 1f) 1f else 2f })
            }
            .pointerInput(photo.file) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                            zoom = (zoom - delta * 0.15f).coerceIn(1f, 4f)
                        }
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = photo.file.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatSize(photo.sizeBytes)} · ${photo.folderName}" +
                        if (zoom > 1f) " · ${"%.1f".format(zoom)}x" else "",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                )
            }
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Kapat",
                tint = Color.White,
                modifier = Modifier.size(26.dp).clickable { onClose() },
            )
        }

        Box(Modifier.fillMaxSize().padding(top = 64.dp, bottom = 40.dp), contentAlignment = Alignment.Center) {
            when {
                loading -> CircularProgressIndicator(color = Color.White)
                bitmap == null -> Text("Görsel açılamadı", color = Color.White.copy(alpha = 0.7f))
                else -> Image(
                    bitmap = bitmap!!,
                    contentDescription = photo.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            }
        }

        previous?.let { track ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 14.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onNavigate(track) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.NavigateBefore, contentDescription = "Önceki", tint = Color.White)
            }
        }
        next?.let { track ->
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 14.dp)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable { onNavigate(track) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.NavigateNext, contentDescription = "Sonraki", tint = Color.White)
            }
        }

        Text(
            text = "${index + 1} / ${photos.size}",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
        )
    }
}

@Composable
fun VideoPlayerOverlay(
    track: Track,
    engine: VideoEngine,
    onClose: () -> Unit,
) {
    LaunchedEffect(track.file) {
        engine.play(track.file)
    }
    DisposableEffect(Unit) {
        onDispose { engine.stop() }
    }

    var paused by remember(track.file) { mutableStateOf(false) }
    var scrubbing by remember(track.file) { mutableStateOf(false) }
    var scrubValue by remember(track.file) { mutableStateOf(0f) }

    val duration = engine.duration
    val maxSeconds = duration.coerceAtLeast(0.01f)
    val shown = if (scrubbing) scrubValue else engine.position

    Box(Modifier.fillMaxSize().background(Color(0xFF04050A))) {
        Box(Modifier.fillMaxSize().padding(top = 56.dp, bottom = 96.dp), contentAlignment = Alignment.Center) {
            val frame = engine.frame
            if (frame != null) {
                Image(
                    bitmap = frame,
                    contentDescription = track.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (engine.failed) "Video açılamadı" else "Yükleniyor…",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = listOfNotNull(
                        formatSize(track.sizeBytes),
                        track.folderName.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                )
            }
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Kapat",
                tint = Color.White,
                modifier = Modifier.size(26.dp).clickable { onClose() },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatTime(shown.toDouble()), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                Slider(
                    value = shown.coerceIn(0f, maxSeconds),
                    onValueChange = {
                        scrubbing = true
                        scrubValue = it
                    },
                    onValueChangeFinished = {
                        scrubbing = false
                        engine.play(track.file, scrubValue.toDouble())
                        paused = false
                    },
                    valueRange = 0f..maxSeconds,
                    modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                )
                Text(formatTime(duration.toDouble()), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            if (paused) {
                                engine.resume()
                            } else {
                                engine.pause()
                            }
                            paused = !paused
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .clickable {
                            engine.play(track.file, 0.0)
                            paused = false
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("Baştan", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}
