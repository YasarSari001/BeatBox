package com.blackstack.beatbox.ui.lyrics
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.theme.AuroraBlack
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode
import kotlinx.coroutines.launch
import com.blackstack.beatbox.ui.components.AlbumArtImage
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.lyrics.LyricsPreferences
import com.blackstack.beatbox.lyrics.LyricsState
import com.blackstack.beatbox.lyrics.LyricsViewModel
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.components.formatTime

/**
 * Aurora Glass şarkı sözleri ekranı.
 *
 * Albüm kapağından türeyen bulanık bir tuval, cam üst çubuk ve alt taşıma
 * çubuğu kullanır. Aktif satır karaoke olarak renklenir; listeye dokunmak
 * otomatik kaydırmayı geçici olarak durdurur. Üst çubuktan aşağı sürüklemek
 * ekranı kapatır.
 *
 * @param durationMs Alt taşıma çubuğundaki ilerleme aralığı.
 * @param onPlayPause Verilirse alt çubukta oynat/duraklat düğmesi görünür.
 * @param onNext Verilirse alt çubukta sonraki düğmesi görünür.
 * @param onPrev Verilirse alt çubukta önceki düğmesi görünür.
 */
@Composable
fun LyricsScreen(
    audio: AudioFile,
    currentPositionMs: Long,
    isPlaying: Boolean,
    viewModel: LyricsViewModel,
    strings: Strings,
    onSeekTo: ((Long) -> Unit)? = null,
    onDismiss: () -> Unit,
    durationMs: Long = audio.duration,
    onPlayPause: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null,
    onPrev: (() -> Unit)? = null
) {
    val lyricsState by viewModel.lyricsState.collectAsState()
    val currentLineIndex by viewModel.currentLineIndex.collectAsState()
    // Konum yalnızca aktif satırda okunur; 100 ms'lik güncellemeler tüm
    // listeyi yeniden çizmez.
    val positionState = rememberUpdatedState(currentPositionMs)

    var showManualSearchDialog by remember { mutableStateOf(false) }
    var searchTrackQuery by remember(audio.id) { mutableStateOf(audio.title) }
    var searchArtistQuery by remember(audio.id) {
        mutableStateOf(
            audio.artist.takeIf { it.isNotBlank() && it != "<unknown>" }.orEmpty()
        )
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val reducedMotion = LocalReducedMotion.current
    // Bu ekranın arka planı üç tam ekran katman çizer; zayıf cihazda
    // animasyonlar ve blur sürekli kare harcar.
    val performanceMode = rememberPerformanceMode()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val prefs = remember(ctx) {
        ctx.getSharedPreferences(LyricsPreferences.PREFS_NAME, Context.MODE_PRIVATE)
    }
    var lyricsPrefs by remember { mutableStateOf(LyricsPreferences.load(prefs)) }
    var showOptions by remember { mutableStateOf(false) }
    val dragOffset = remember { Animatable(0f) }
    val dismissThreshold = with(density) { 120.dp.toPx() }

    BackHandler(onBack = onDismiss)

    // Şarkı değişince sözleri getir; konum değişince aktif satırı güncelle.
    LaunchedEffect(audio.id) { viewModel.loadLyrics(audio) }
    LaunchedEffect(currentPositionMs) { viewModel.updatePlaybackPosition(currentPositionMs) }

    val latestOnSeekTo = rememberUpdatedState(onSeekTo)
    val seekTo: (Long) -> Unit = remember(viewModel) {
        { ms: Long ->
            latestOnSeekTo.value?.invoke(ms)
            viewModel.updatePlaybackPosition(ms)
        }
    }

    val artistLabel = audio.artist
        .takeIf { it.isNotBlank() && it != "<unknown>" }
        ?: strings.get("unknown_artist")

    // Nefes alan albüm kapağı ve yavaş ışık taraması. Performans modunda
    // sonsuz animasyon hiç kurulmaz; değerler sabit kalır ve kare üretilmez.
    val ambient = if (reducedMotion || performanceMode) {
        null
    } else {
        rememberInfiniteTransition(label = "lyrics_ambient")
    }
    val breath: Float
    val sweep: Float
    if (ambient == null) {
        breath = 1f
        sweep = -0.55f
    } else {
        breath = ambient.animateFloat(
            initialValue = 1f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(6_000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lyrics_ambient_breath"
        ).value
        sweep = ambient.animateFloat(
            initialValue = -0.55f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(9_000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lyrics_ambient_sweep"
        ).value
    }

    Box(modifier = Modifier.fillMaxSize().background(AuroraBlack)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = dragOffset.value
                    val fade = (dragOffset.value / (size.height * 0.55f)).coerceIn(0f, 0.5f)
                    alpha = 1f - fade
                }
        ) {
            // 1) Albüm kapağı: API 31+ cihazlarda gerçek blur, eski cihazlarda
            //    düşük opaklıkta doku olarak kalır. Performans modunda blur
            //    tam ekran render effect maliyeti nedeniyle atlanır.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = breath
                        scaleY = breath
                        alpha = if (performanceMode) 0.10f else 0.18f
                    }
            ) {
                AlbumArtImage(
                    a = audio,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (performanceMode) Modifier else Modifier.blur(96.dp))
                )
            }

            // 2) Aurora ışıkları: tema renklerinden iki yumuşak leke.
            //    Fırçalar boyut değişmedikçe yeniden kurulmaz.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val topGlow = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.30f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.12f, size.height * 0.16f),
                            radius = size.width * 1.15f
                        )
                        val bottomGlow = Brush.radialGradient(
                            colors = listOf(
                                secondaryColor.copy(alpha = 0.20f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.95f, size.height * 0.64f),
                            radius = size.width * 1.0f
                        )
                        onDrawBehind {
                            drawRect(brush = topGlow)
                            drawRect(brush = bottomGlow)
                        }
                    }
            )

            // 3) Yavaş ışık taraması: yalnızca animasyon zamanlayıcısı yokken
            //    anlamlıdır; performans modunda katman hiç eklenmez.
            if (!performanceMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawBehind {
                            val centerX = size.width * (0.5f + sweep)
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.05f),
                                        Color.Transparent
                                    ),
                                    start = Offset(centerX - size.width * 0.4f, 0f),
                                    end = Offset(centerX + size.width * 0.4f, size.height)
                                )
                            )
                        }
                )
            }

            // 4) Okunabilirlik için vinyet.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.64f),
                            0.30f to Color.Black.copy(alpha = 0.16f),
                            0.72f to Color.Black.copy(alpha = 0.26f),
                            1f to Color.Black.copy(alpha = 0.88f)
                        )
                    )
            )

            // 5) İçerik.
            when (val state = lyricsState) {
                is LyricsState.Success -> {
                    if (state.lyrics.lines.isEmpty()) {
                        LyricsErrorState(
                            message = strings.get("no_lyrics_found"),
                            strings = strings,
                            primaryColor = primaryColor,
                            onRetry = { viewModel.retry(audio) },
                            onManualSearch = { showManualSearchDialog = true }
                        )
                    } else {
                        LyricsLines(
                            lyrics = state.lyrics,
                            currentLineIndex = currentLineIndex,
                            positionState = positionState,
                            primaryColor = primaryColor,
                            strings = strings,
                            onSeek = seekTo,
                            textSizeSp = lyricsPrefs.textSizeSp,
                            glowEnabled = lyricsPrefs.glowEnabled,
                            clickEnabled = lyricsPrefs.clickToSeek
                        )
                    }
                }

                is LyricsState.Error -> LyricsErrorState(
                    message = state.message,
                    strings = strings,
                    primaryColor = primaryColor,
                    onRetry = { viewModel.retry(audio) },
                    onManualSearch = { showManualSearchDialog = true }
                )

                LyricsState.Loading, LyricsState.Idle -> LyricsLoadingState(strings)
            }

            // 6) Üst karartma: söz satırları durum çubuğunun altında kaybolur.
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to AuroraBlack,
                            0.55f to AuroraBlack.copy(alpha = 0.92f),
                            1f to Color.Transparent
                        )
                    )
            )

            // 7) Cam üst çubuk. Aşağı sürüklemek ekranı kapatır.
            LyricsTopBar(
                title = audio.title,
                artist = artistLabel,
                source = (lyricsState as? LyricsState.Success)?.lyrics?.metadata?.source,
                strings = strings,
                onDismiss = onDismiss,
                onSearch = { showManualSearchDialog = true },
                onOptions = { showOptions = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragEnd = {
                                if (dragOffset.value > dismissThreshold) {
                                    onDismiss()
                                } else {
                                    scope.launch {
                                        dragOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.82f,
                                                stiffness = 420f
                                            )
                                        )
                                    }
                                }
                            },
                            onVerticalDrag = { change, amount ->
                                if (amount > 0f || dragOffset.value > 0f) {
                                    change.consume()
                                    scope.launch {
                                        dragOffset.snapTo(
                                            (dragOffset.value + amount).coerceAtLeast(0f)
                                        )
                                    }
                                }
                            }
                        )
                    }
            )

            // 8) Alt karartma: cam taşıma çubuğunun arkasını okunaklı yapar.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(310.dp)
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.34f to AuroraBlack.copy(alpha = 0.90f),
                            1f to AuroraBlack
                        )
                    )
            )

            // 9) Cam alt taşıma çubuğu.
            LyricsTransportBar(
                positionMs = currentPositionMs,
                durationMs = durationMs,
                isPlaying = isPlaying,
                strings = strings,
                onSeek = seekTo,
                onPlayPause = onPlayPause,
                onNext = onNext,
                onPrev = onPrev,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }

    if (showOptions) {
        LyricsOptionsSheet(
            strings = strings,
            prefs = prefs,
            current = lyricsPrefs,
            onPrefsChange = { lyricsPrefs = it },
            onReload = {
                showOptions = false
                viewModel.refresh(audio)
            },
            onCopy = {
                val lyrics = (lyricsState as? LyricsState.Success)?.lyrics
                val text = lyrics?.lines?.joinToString("\n") { it.text }
                if (!text.isNullOrBlank()) {
                    val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText(audio.title, text))
                    Toast.makeText(ctx, strings.get("lyrics_copied"), Toast.LENGTH_SHORT).show()
                }
                showOptions = false
            },
            onDismiss = { showOptions = false }
        )
    }

    if (showManualSearchDialog) {
        ManualLyricsSearchDialog(
            trackQuery = searchTrackQuery,
            artistQuery = searchArtistQuery,
            strings = strings,
            primaryColor = primaryColor,
            onTrackQueryChange = { searchTrackQuery = it },
            onArtistQueryChange = { searchArtistQuery = it },
            onDismiss = { showManualSearchDialog = false },
            onConfirm = {
                showManualSearchDialog = false
                viewModel.searchLyricsManually(searchTrackQuery, searchArtistQuery)
            }
        )
    }
}

@Composable
private fun LyricsTopBar(
    title: String,
    artist: String,
    source: String?,
    strings: Strings,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(AuroraBlack.copy(alpha = 0.78f))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.035f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.get("close"),
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = artist,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            if (!source.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = source.uppercase(),
                    color = primary.copy(alpha = 0.78f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.4.sp,
                    maxLines = 1
                )
            }
        }

        IconButton(
            onClick = onSearch,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = strings.get("manual_search"),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(Modifier.width(6.dp))

        IconButton(
            onClick = onOptions,
            modifier = Modifier
                .size(48.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = strings.get("lyrics_options"),
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsTransportBar(
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    strings: Strings,
    onSeek: (Long) -> Unit,
    onPlayPause: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onPrev: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val safeDuration = durationMs.coerceAtLeast(1L)

    // Sürükleme sırasında yerel değer kullanılır; her karede player.seekTo
    // çağrılmaz, yalnızca bırakınca atlanır.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val displayValue = (dragValue ?: positionMs.toFloat())
        .coerceIn(0f, safeDuration.toFloat())

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(AuroraBlack.copy(alpha = 0.80f))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.035f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        Slider(
            value = displayValue,
            onValueChange = { dragValue = it },
            onValueChangeFinished = {
                dragValue?.let { onSeek(it.toLong()) }
                dragValue = null
            },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = primary,
                inactiveTrackColor = Color.White.copy(alpha = 0.14f)
            ),
            thumb = {
                Box(
                    Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = SliderDefaults.colors(
                        activeTrackColor = primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.14f),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    drawStopIndicator = {}
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(displayValue.toLong()),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = formatTime(safeDuration),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onPrev != null) {
                IconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = strings.get("previous"),
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(Modifier.width(22.dp))

            if (onPlayPause != null) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(listOf(primary, secondary))
                        )
                        .clickable(role = Role.Button, onClick = onPlayPause),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = strings.get(if (isPlaying) "pause" else "play"),
                        tint = AuroraBlack,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Spacer(Modifier.width(22.dp))

            if (onNext != null) {
                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = strings.get("next"),
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualLyricsSearchDialog(
    trackQuery: String,
    artistQuery: String,
    strings: Strings,
    primaryColor: Color,
    onTrackQueryChange: (String) -> Unit,
    onArtistQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(strings.get("search"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = strings.get("cancel"),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.get("manual_search"),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = trackQuery,
                    onValueChange = onTrackQueryChange,
                    label = { Text(strings.get("track_name")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor
                    )
                )
                OutlinedTextField(
                    value = artistQuery,
                    onValueChange = onArtistQueryChange,
                    label = { Text(strings.get("artist")) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = primaryColor,
                        focusedLabelColor = primaryColor
                    )
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(26.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LyricsOptionsSheet(
    strings: Strings,
    prefs: SharedPreferences,
    current: LyricsPreferences,
    onPrefsChange: (LyricsPreferences) -> Unit,
    onReload: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = AuroraBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = strings.get("lyrics_options"),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = strings.get("lyrics_providers"),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp
            )
            LyricsPreferences.PROVIDERS.forEach { provider ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = provider,
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = provider in current.enabledProviders,
                        onCheckedChange = { checked ->
                            val updated = if (checked) {
                                current.enabledProviders + provider
                            } else {
                                current.enabledProviders - provider
                            }
                            // En az bir kaynak açık kalmalı.
                            if (updated.isNotEmpty()) {
                                LyricsPreferences.saveProviders(prefs, updated)
                                onPrefsChange(current.copy(enabledProviders = updated))
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = strings.get("lyrics_size_hint")
                    .replace("%s", current.textSizeSp.toInt().toString()),
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 13.sp
            )
            Slider(
                value = current.textSizeSp,
                onValueChange = { size ->
                    // Sürükleme sırasında hem ekran hem tercih güncellenir;
                    // "finished" anında eski composition değeri yakalanmaz.
                    LyricsPreferences.saveTextSize(prefs, size)
                    onPrefsChange(current.copy(textSizeSp = size))
                },
                valueRange = LyricsPreferences.MIN_TEXT_SIZE..LyricsPreferences.MAX_TEXT_SIZE,
                steps = 8,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.get("lyrics_glow"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = current.glowEnabled,
                    onCheckedChange = { checked ->
                        LyricsPreferences.saveGlow(prefs, checked)
                        onPrefsChange(current.copy(glowEnabled = checked))
                    }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.get("lyrics_click_seek"),
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = current.clickToSeek,
                    onCheckedChange = { checked ->
                        LyricsPreferences.saveClickToSeek(prefs, checked)
                        onPrefsChange(current.copy(clickToSeek = checked))
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReload,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(strings.get("lyrics_reload"), fontSize = 12.sp, maxLines = 1)
                }
                OutlinedButton(
                    onClick = onCopy,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(strings.get("lyrics_copy"), fontSize = 12.sp, maxLines = 1)
                }
            }
        }
    }
}
