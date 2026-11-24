package com.blackstack.beatbox.ui.lyrics
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
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
import androidx.compose.runtime.State
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.components.EmptyArtKind
import com.blackstack.beatbox.ui.components.EmptyStateArt
import com.blackstack.beatbox.ui.theme.BeatBoxMotion
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.roundToInt
import com.blackstack.beatbox.lyrics.LyricsWord
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.lyrics.SyncedLyrics
import com.blackstack.beatbox.lyrics.SyncedLyricsLine

/** Aktif satırın merkezinin viewport içindeki dikey çapa noktası. */
internal const val LyricsAnchorFraction = 0.40f

/** Satırlar arası kaydırmanın süresi; iki aşamalı animasyonun yerini alır. */
private const val LyricsScrollDuration = 320

/** Varsayılan metrikler; kullanıcı ayarıyla ölçeklenir. */
private const val LyricsDefaultFontSize = 24f

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
 * Kelime zaman damgaları varsa karaoke boyamasını kelime sınırlarına oturtur.
 *
 * Metrolist'in `WordTimestamp` tabanlı vurgulamasından uyarlanmıştır. Kelime
 * içinde de yumuşak ilerleme uygulanır; böylece uzun kelimeler aniden
 * boyanmaz. Zaman damgası olmayan satırlar için [karaokeSungChars] kullanılır.
 *
 * @param words Satırın kelimeleri (metinleri birleşince satır metnini verir)
 * @param positionMs Çalan parçanın konumu
 * @param textLength Satır metninin karakter sayısı
 * @param lineEndMs Satırın bitiş zamanı; son kelimenin süresi için kullanılır
 */
internal fun karaokeSungCharsForWords(
    words: List<LyricsWord>,
    positionMs: Long,
    textLength: Int,
    lineEndMs: Long
): Int {
    if (textLength <= 0 || words.isEmpty()) return 0

    var sung = 0
    words.forEachIndexed { index, word ->
        val wordStart = word.startMs.coerceAtLeast(0L)
        val nextStart = words.getOrNull(index + 1)?.startMs
            ?.coerceAtLeast(wordStart + 1L)
            ?: lineEndMs.coerceAtLeast(wordStart + 1L)
        val wordLength = word.text.length

        if (positionMs < wordStart) {
            return sung.coerceIn(0, textLength)
        }
        if (positionMs >= nextStart) {
            sung += wordLength
            return@forEachIndexed
        }

        val fraction = ((positionMs - wordStart).toFloat() /
            (nextStart - wordStart).toFloat()).coerceIn(0f, 1f)
        sung += (fraction * wordLength).toInt()
        return sung.coerceIn(0, textLength)
    }

    return sung.coerceIn(0, textLength)
}

/**
 * Görünür bir satırı çapa noktasına getirmek için gereken kaydırma miktarı.
 * Pozitif değer içeriği yukarı, negatif değer aşağı kaydırır.
 */
internal fun lyricsScrollDelta(
    itemOffset: Int,
    itemSize: Int,
    viewportStart: Int,
    viewportHeight: Int,
    anchorFraction: Float = LyricsAnchorFraction
): Float {
    val desiredCenter = viewportStart + viewportHeight * anchorFraction
    val itemCenter = itemOffset + itemSize / 2f
    return itemCenter - desiredCenter
}

/**
 * Görünür olmayan bir satıra atlarken kullanılacak offset.
 *
 * `scrollToItem` offset'i satırın üst kenarını viewport başlangıcına göre
 * kaydırır; negatif değer satırı aşağıda konumlandırır.
 */
internal fun lyricsItemScrollOffset(
    estimatedItemSize: Int,
    viewportHeight: Int,
    anchorFraction: Float = LyricsAnchorFraction
): Int = (estimatedItemSize / 2f - viewportHeight * anchorFraction).roundToInt()

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
    positionState: State<Long>,
    primaryColor: Color,
    strings: Strings,
    onSeek: (Long) -> Unit,
    textSizeSp: Float = LyricsDefaultFontSize,
    glowEnabled: Boolean = true,
    clickEnabled: Boolean = true
) {
    val textSize = textSizeSp.sp
    val lineHeight = (textSizeSp * 1.5f).sp
    val listState = rememberLazyListState()
    val reducedMotion = LocalReducedMotion.current
    val density = LocalDensity.current
    var userScrolling by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }

    // Kullanıcı her dokunuşta otomatik kaydırmayı 4 saniye durdurur.
    LaunchedEffect(interactionTick) {
        if (interactionTick > 0) {
            delay(4_000)
            userScrolling = false
        }
    }

    // Aktif satırı tek bir yumuşak hareketle çapa noktasına getir.
    //
    // Önceden önce animateScrollToItem (satır tepeye) sonra animateScrollBy
    // (geri aşağı) çalışıyordu; bu iki aşama "yukarı-aşağı" titremesi
    // oluşturuyordu. Satır zaten görünürken yalnızca hedefe olan fark kadar
    // kaydırmak tek ve kesintisiz bir animasyon verir.
    LaunchedEffect(currentLineIndex, userScrolling, lyrics.lines.size, reducedMotion) {
        if (userScrolling || currentLineIndex !in lyrics.lines.indices) return@LaunchedEffect

        val viewportHeight = snapshotFlow {
            listState.layoutInfo.viewportEndOffset - listState.layoutInfo.viewportStartOffset
        }.first { it > 0 }

        val layoutInfo = listState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == currentLineIndex }

        if (itemInfo != null) {
            val delta = lyricsScrollDelta(
                itemOffset = itemInfo.offset,
                itemSize = itemInfo.size,
                viewportStart = layoutInfo.viewportStartOffset,
                viewportHeight = viewportHeight
            )
            if (abs(delta) >= 1f) {
                if (reducedMotion) {
                    listState.scrollBy(delta)
                } else {
                    listState.animateScrollBy(
                        value = delta,
                        animationSpec = tween(
                            durationMillis = LyricsScrollDuration,
                            easing = FastOutSlowInEasing
                        )
                    )
                }
            }
        } else {
            // Uzak bir ana atlandıysa satırı tek animasyonla konumlandır.
            // En yakın görünür satırın yüksekliği iyi bir tahmin verir.
            val estimatedSize = layoutInfo.visibleItemsInfo
                .minByOrNull { abs(it.index - currentLineIndex) }?.size
                ?: with(density) { 58.dp.toPx().toInt() }
            val scrollOffset = lyricsItemScrollOffset(estimatedSize, viewportHeight)
            if (reducedMotion) {
                listState.scrollToItem(currentLineIndex, scrollOffset)
            } else {
                listState.animateScrollToItem(currentLineIndex, scrollOffset)
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
                positionState = positionState,
                lineDurationMs = lineDurationMs,
                primaryColor = primaryColor,
                strings = strings,
                textSize = textSize,
                lineHeight = lineHeight,
                glowEnabled = glowEnabled,
                clickEnabled = clickEnabled,
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
    positionState: State<Long>,
    lineDurationMs: Long,
    primaryColor: Color,
    strings: Strings,
    textSize: TextUnit,
    lineHeight: TextUnit,
    glowEnabled: Boolean,
    clickEnabled: Boolean,
    onClick: () -> Unit
) {
    val glowShadow = if (glowEnabled && isCurrent) {
        androidx.compose.ui.graphics.Shadow(
            color = primaryColor.copy(alpha = 0.55f),
            blurRadius = 18f
        )
    } else {
        null
    }
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
                enabled = clickEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 10.dp),
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
                    // Konum yalnızca aktif satırda okunur; diğer satırlar
                    // 100 ms'lik konum güncellemelerinde yeniden çizilmez.
                    val positionMs = positionState.value
                    val sungChars = if (line.words.isNotEmpty()) {
                        karaokeSungCharsForWords(
                            words = line.words,
                            positionMs = positionMs,
                            textLength = line.text.length,
                            lineEndMs = line.timestampMs + lineDurationMs
                        )
                    } else {
                        val progress = ((positionMs - line.timestampMs).toFloat() /
                            lineDurationMs.toFloat()).coerceIn(0f, 1f)
                        karaokeSungChars(line.text.length, progress)
                    }
                    KaraokeText(
                        text = line.text,
                        sungChars = sungChars,
                        primaryColor = primaryColor,
                        fontSize = textSize,
                        lineHeight = lineHeight,
                        glow = if (isCurrent) glowShadow else null
                    )
                }

                else -> Text(
                    text = line.text,
                    color = Color.White.copy(alpha = alpha),
                    fontSize = textSize,
                    lineHeight = lineHeight,
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
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = if (isCurrent) strings.get("lyrics_intro") else "♪ ♪ ♪",
            color = if (isCurrent) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = alpha),
            fontSize = 20.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.SemiBold
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
    sungChars: Int,
    primaryColor: Color,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    glow: androidx.compose.ui.graphics.Shadow?
) {
    val annotated = remember(text, sungChars, primaryColor) {
        buildAnnotatedString {
            if (sungChars > 0) {
                withStyle(
                    SpanStyle(color = Color.White.copy(alpha = 0.94f))
                ) {
                    append(text.substring(0, sungChars))
                }
            }
            if (sungChars < text.length) {
                withStyle(
                    SpanStyle(color = primaryColor)
                ) {
                    append(text[sungChars])
                }
                if (sungChars + 1 < text.length) {
                    withStyle(
                        SpanStyle(color = Color.White.copy(alpha = 0.30f))
                    ) {
                        append(text.substring(sungChars + 1))
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.SemiBold,
        style = androidx.compose.ui.text.TextStyle(shadow = glow),
        modifier = Modifier.fillMaxWidth()
    )
}

/** Yükleniyor: cam kart içinde nabız atan iskelet satırlar. */
@Composable
internal fun LyricsLoadingState(strings: Strings) {
    val reducedMotion = LocalReducedMotion.current
    val performanceMode = rememberPerformanceMode()
    val pulse: Float
    if (reducedMotion || performanceMode) {
        pulse = 0.35f
    } else {
        val transition = rememberInfiniteTransition(label = "lyrics_loading")
        pulse = transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1_100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "lyrics_loading_pulse"
        ).value
    }
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

/** Hata / söz bulunamadı: cam kart, illüstrasyon ve iki yan yana eylem. */
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
            .padding(start = 24.dp, end = 24.dp, top = 108.dp, bottom = 188.dp),
        contentAlignment = Alignment.Center
    ) {
        LyricsGlassCard(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateArt(
                    kind = EmptyArtKind.SEARCH,
                    size = 100.dp,
                    accent = primaryColor,
                    secondaryAccent = secondary
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = strings.get("no_lyrics_found"),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .background(
                            Brush.horizontalGradient(listOf(primaryColor, secondary)),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.get("retry"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = onManualSearch,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.16f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    contentPadding = PaddingValues(horizontal = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.get("manual_search"),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
