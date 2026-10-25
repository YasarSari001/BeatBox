package com.blackstack.beatbox

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Karaoke ilerlemesini karakter indeksine çevirir.
 *
 * Ayrı bir fonksiyon olması, satır içi hesaplamayı test edilebilir kılar.
 */
internal fun karaokeSungChars(textLength: Int, progress: Float): Int {
    if (textLength <= 0) return 0
    return (progress.coerceIn(0f, 1f) * textLength).toInt().coerceIn(0, textLength)
}

/**
 * Senkron söz listesi.
 *
 * Aktif satır ekranın yaklaşık üçte birinde tutulur. Kullanıcı listeye
 * dokunduğunda otomatik kaydırma 4 saniye duraklar; böylece geçmişe bakmak
 * mümkün olur. Satıra dokunmak o ana atlar.
 */
@Composable
internal fun LyricsLines(
    lyrics: SyncedLyrics,
    currentLineIndex: Int,
    currentPositionMs: Long,
    primaryColor: Color,
    strings: Strings,
    onSeek: (Long) -> Unit
) {
    val listState = rememberLazyListState()
    val reducedMotion = LocalReducedMotion.current
    var userScrolling by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    // Kullanıcı her dokunuşta otomatik kaydırmayı 4 saniye durdurur.
    LaunchedEffect(interactionTick) {
        if (interactionTick > 0) {
            delay(4_000)
            userScrolling = false
        }
    }

    // Aktif satırı ekranın üst-orta bölgesine getir. viewport ölçülmeden
    // animateScrollToItem çağrılmaz; aksi halde satır üst çubuğun altında kalır.
    LaunchedEffect(currentLineIndex, userScrolling, lyrics.lines.size) {
        if (!userScrolling && currentLineIndex in lyrics.lines.indices) {
            snapshotFlow {
                listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
            }.first { it > 0 }

            // Önce satırı görünür alana getir, sonra gerçek ölçüsüne göre
            // ekranın üst-orta bölgesine kaydır. Sabit offset kullanmak,
            // farklı satır yüksekliklerinde (ör. iki satıra taşan sözler)
            // yanlış konumlamaya yol açıyordu.
            listState.animateScrollToItem(currentLineIndex)
            val itemInfo = listState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index == currentLineIndex }
            if (itemInfo != null) {
                val viewportStart = listState.layoutInfo.viewportStartOffset
                val viewportEnd = listState.layoutInfo.viewportEndOffset
                val desiredCenter = viewportStart + (viewportEnd - viewportStart) * 0.40f
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val target = itemCenter - desiredCenter
                if (target != 0f) {
                    listState.animateScrollBy(target)
                }
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Tüketmeden dinle: LazyColumn kaydırması bozulmaz.
                    awaitFirstDown(requireUnconsumed = false)
                    userScrolling = true
                    interactionTick++
                    var pressed = true
                    while (pressed) {
                        val event = awaitPointerEvent()
                        pressed = event.changes.any { it.pressed }
                    }
                }
            },
        contentPadding = PaddingValues(
            start = 24.dp,
            end = 24.dp,
            top = 148.dp,
            bottom = 268.dp
        ),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        itemsIndexed(
            items = lyrics.lines,
            key = { index, line -> "${line.timestampMs}_$index" }
        ) { index, line ->
            val isCurrent = index == currentLineIndex
            val isPast = index < currentLineIndex
            val nextTimestamp = lyrics.lines.getOrNull(index + 1)?.timestampMs
                ?: (line.timestampMs + 4_000L)
            val lineDurationMs = (nextTimestamp - line.timestampMs).coerceAtLeast(1_200L)

            LyricsLine(
                line = line,
                isCurrent = isCurrent,
                isPast = isPast,
                currentPositionMs = currentPositionMs,
                lineDurationMs = lineDurationMs,
                primaryColor = primaryColor,
                reducedMotion = reducedMotion,
                strings = strings,
                onClick = {
                    userScrolling = false
                    onSeek(line.timestampMs)
                }
            )
        }
    }
}

@Composable
private fun LyricsLine(
    line: SyncedLyricsLine,
    isCurrent: Boolean,
    isPast: Boolean,
    currentPositionMs: Long,
    lineDurationMs: Long,
    primaryColor: Color,
    reducedMotion: Boolean,
    strings: Strings,
    onClick: () -> Unit
) {
    val targetAlpha = when {
        isCurrent -> 1f
        isPast -> 0.34f
        else -> 0.50f
    }
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(BeatBoxMotion.Standard),
        label = "lyrics_line_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (isCurrent && !reducedMotion) 1.015f else 1f,
        animationSpec = BeatBoxMotion.Soft,
        label = "lyrics_line_scale"
    )
    val accentAlpha by animateFloatAsState(
        targetValue = if (isCurrent) 1f else 0f,
        animationSpec = tween(BeatBoxMotion.Standard),
        label = "lyrics_line_accent"
    )

    val isIntro = remember(line.text) {
        line.text.contains("♪") || line.text.equals("intro", ignoreCase = true)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0.5f)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aktif satırın solundaki aurora çizgisi; satır değişince kaymaz.
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            primaryColor.copy(alpha = accentAlpha),
                            primaryColor.copy(alpha = accentAlpha * 0.18f)
                        )
                    )
                )
        )
        Spacer(Modifier.width(13.dp))

        Box(modifier = Modifier.weight(1f)) {
            when {
                isIntro -> LyricsIntroLine(
                    isCurrent = isCurrent,
                    alpha = alpha,
                    primaryColor = primaryColor,
                    strings = strings
                )

                isCurrent -> {
                    val progress = ((currentPositionMs - line.timestampMs).toFloat() /
                        lineDurationMs.toFloat()).coerceIn(0f, 1f)
                    KaraokeText(
                        text = line.text,
                        progress = progress,
                        primaryColor = primaryColor
                    )
                }

                else -> Text(
                    text = line.text,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = 22.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun LyricsIntroLine(
    isCurrent: Boolean,
    alpha: Float,
    primaryColor: Color,
    strings: Strings
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.GraphicEq,
            contentDescription = null,
            tint = if (isCurrent) primaryColor else Color.White.copy(alpha = alpha * 0.7f),
            modifier = Modifier.size(if (isCurrent) 28.dp else 22.dp)
        )
        Text(
            text = if (isCurrent) strings.get("lyrics_intro") else "♪ ♪ ♪",
            color = if (isCurrent) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = alpha),
            fontSize = if (isCurrent) 22.sp else 19.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
        )
    }
}

/**
 * Karaoke metni: geçmiş karakterler beyaz, aktif karakter tema rengi,
 * gelecek karakterler soluk. Tek bir [Text] kullanıldığı için uzun satırlar
 * ve boşluksuz diller (CJK) doğru şekilde satır atlar.
 */
@Composable
private fun KaraokeText(
    text: String,
    progress: Float,
    primaryColor: Color
) {
    val sungChars = karaokeSungChars(text.length, progress)

    val annotated = remember(text, sungChars, primaryColor) {
        buildAnnotatedString {
            if (sungChars > 0) {
                withStyle(
                    SpanStyle(
                        color = Color.White.copy(alpha = 0.94f),
                        fontWeight = FontWeight.SemiBold
                    )
                ) {
                    append(text.substring(0, sungChars))
                }
            }
            if (sungChars < text.length) {
                withStyle(
                    SpanStyle(
                        color = primaryColor,
                        fontWeight = FontWeight.ExtraBold
                    )
                ) {
                    append(text[sungChars])
                }
                if (sungChars + 1 < text.length) {
                    withStyle(
                        SpanStyle(
                            color = Color.White.copy(alpha = 0.30f),
                            fontWeight = FontWeight.Medium
                        )
                    ) {
                        append(text.substring(sungChars + 1))
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = 26.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Yükleniyor: cam kart içinde nabız atan iskelet satırlar. */
@Composable
internal fun LyricsLoadingState(strings: Strings) {
    val reducedMotion = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "lyrics_loading")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (reducedMotion) 0.35f else 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "lyrics_loading_pulse"
    )
    val widths = listOf(0.92f, 0.74f, 0.86f, 0.58f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        LyricsGlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.get("loading_lyrics"),
                color = Color.White.copy(alpha = 0.82f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(22.dp))
            widths.forEachIndexed { index, fraction ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(14.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Color.White.copy(alpha = if (reducedMotion) 0.10f else 0.08f + pulse * 0.10f)
                        )
                )
                if (index != widths.lastIndex) Spacer(Modifier.height(13.dp))
            }
        }
    }
}

/** Hata / söz bulunamadı: cam kart, gradyan ikon ve iki eylem. */
@Composable
internal fun LyricsErrorState(
    message: String,
    strings: Strings,
    primaryColor: Color,
    onRetry: () -> Unit,
    onManualSearch: () -> Unit
) {
    val secondary = MaterialTheme.colorScheme.secondary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        LyricsGlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                primaryColor.copy(alpha = 0.32f),
                                secondary.copy(alpha = 0.18f)
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicOff,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = strings.get("no_lyrics_found"),
                color = Color.White,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = 4
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(
                        Brush.horizontalGradient(listOf(primaryColor, secondary)),
                        RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(horizontal = 18.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.get("retry"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onManualSearch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.16f)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = strings.get("manual_search"),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
internal fun LyricsGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.10f),
                        Color.White.copy(alpha = 0.035f)
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
            .padding(24.dp),
        content = content
    )
}
