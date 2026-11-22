/*
 * BeatBox
 *
 * Bu dosya, GPL-3.0 lisansı altında yayımlanan Metrolist projesinden
 * (https://github.com/MetrolistGroup/Metrolist) uyarlanmıştır.
 * Uyarlanmış bu dosya da GPL-3.0 koşulları altında dağıtılır.
 *
 * This file is adapted from the Metrolist project, licensed under GPL-3.0.
 * This adapted file is distributed under the terms of GPL-3.0.
 */
package com.blackstack.beatbox.ui.player

import android.os.Build
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.components.AlbumArtCache
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import com.blackstack.beatbox.ui.theme.rememberPerformanceMode
import kotlinx.coroutines.delay

/**
 * Metrolist oynatıcısındaki gibi tam ekran bulanık albüm kapağı.
 *
 * Kapak bit eşlemi yalnızca bellek önbelleğinde varsa çizilir; ana kapak
 * yüklenirken burada ikinci bir ağ isteği veya yükleme göstergesi oluşmaz.
 * API 31 öncesinde gerçek blur bulunmadığından kapak düşük opaklıkta doku
 * olarak kalır ve tema renklerinden bir gradyan devreye girer.
 */
@Composable
internal fun PlayerBackdrop(audio: AudioFile, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    // Tam ekran blur en pahalı efekt; performans modunda ve eski cihazlarda
    // atlanır, yerine yarı saydam kapak dokusu kullanılır.
    val performanceMode = rememberPerformanceMode()
    val blurSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !performanceMode
    var bitmap by remember(audio.id) { mutableStateOf(AlbumArtCache.bitmapCache.get(audio.id)) }

    LaunchedEffect(audio.id) {
        var attempts = 0
        while (bitmap == null && attempts < 40) {
            delay(150)
            bitmap = AlbumArtCache.bitmapCache.get(audio.id)
            attempts++
        }
    }

    Box(modifier.fillMaxSize().background(Color(0xFF07080D))) {
        bitmap?.let { art ->
            Image(
                bitmap = art.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = if (blurSupported) 1.2f else 1f
                        scaleY = if (blurSupported) 1.2f else 1f
                        alpha = if (blurSupported) 0.64f else 0.18f
                    }
                    .then(if (blurSupported) Modifier.blur(90.dp) else Modifier)
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(primary.copy(alpha = 0.20f), Color.Transparent),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = size.width
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(secondary.copy(alpha = 0.12f), Color.Transparent),
                            center = Offset(size.width * 0.1f, size.height * 0.8f),
                            radius = size.width * 0.9f
                        )
                    )
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.24f),
                            0.45f to Color.Black.copy(alpha = 0.10f),
                            1f to Color.Black.copy(alpha = 0.84f)
                        )
                    )
                }
        )
    }
}

/**
 * Dokununca hafifçe küçülen yuvarlak oynatıcı düğmesi.
 * [LocalReducedMotion] etkinse ölçek animasyonu atlanır.
 */
@Composable
internal fun PlayerScaleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    tint: Color = Color.White,
    containerColor: Color = Color.White.copy(alpha = 0.10f),
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.9f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = 0.55f, stiffness = 700f)
        },
        label = "player_icon_scale"
    )
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(containerColor)
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

/** Oynatıcının üst çubuğu: daralt, "şimdi çalıyor" ve sağdaki eylemler. */
@Composable
internal fun PlayerTopBar(
    strings: Strings,
    onCollapse: () -> Unit,
    onOpenEQ: (() -> Unit)?,
    moreMenu: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.fillMaxWidth().height(52.dp)) {
        PlayerScaleIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = strings.get("collapse"),
            onClick = onCollapse,
            modifier = Modifier.align(Alignment.CenterStart),
            iconSize = 26.dp
        )
        Text(
            text = strings.get("now_playing_badge"),
            modifier = Modifier.align(Alignment.Center),
            color = Color.White.copy(alpha = 0.78f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            maxLines = 1
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onOpenEQ != null) {
                PlayerScaleIconButton(
                    icon = Icons.Rounded.Equalizer,
                    contentDescription = strings.get("equalizer"),
                    onClick = onOpenEQ
                )
            }
            moreMenu()
        }
    }
}

/** Kapak altındaki başlık, sanatçı, favori ve paylaş satırı. */
@Composable
internal fun PlayerTrackInfo(
    audio: AudioFile,
    isFav: Boolean,
    strings: Strings,
    onToggleFav: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    val artistLabel = audio.artist
        .takeIf { it.isNotBlank() && it != "<unknown>" }
        ?: strings.get("unknown_artist")
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = audio.title,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = artistLabel,
                color = Color.White.copy(alpha = 0.68f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(12.dp))
        PlayerScaleIconButton(
            icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
            contentDescription = strings.get("favorite"),
            onClick = onToggleFav,
            tint = if (isFav) Color(0xFFFF4D8D) else Color.White,
            containerColor = if (isFav) {
                Color(0xFFFF4D8D).copy(alpha = 0.18f)
            } else {
                Color.White.copy(alpha = 0.10f)
            }
        )
        Spacer(Modifier.width(8.dp))
        PlayerScaleIconButton(
            icon = Icons.Rounded.Share,
            contentDescription = strings.get("share"),
            onClick = onShare
        )
    }
}

/**
 * Metrolist'teki geniş hap biçimli taşıma kontrolleri:
 * önceki, etiketli oynat/duraklat ve sonraki.
 */
@Composable
internal fun PlayerPrimaryControls(
    isPlaying: Boolean,
    strings: Strings,
    onPrev: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerTransportButton(
            icon = Icons.Rounded.SkipPrevious,
            contentDescription = strings.get("previous"),
            onClick = onPrev,
            modifier = Modifier.weight(0.62f).height(58.dp),
            containerColor = Color.White.copy(alpha = 0.10f),
            contentColor = Color.White
        )
        PlayerTransportButton(
            icon = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = if (isPlaying) strings.get("pause") else strings.get("play"),
            label = if (isPlaying) strings.get("pause") else strings.get("play"),
            onClick = onPlayPause,
            modifier = Modifier.weight(1.9f).height(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
        PlayerTransportButton(
            icon = Icons.Rounded.SkipNext,
            contentDescription = strings.get("next"),
            onClick = onNext,
            modifier = Modifier.weight(0.62f).height(58.dp),
            containerColor = Color.White.copy(alpha = 0.10f),
            contentColor = Color.White
        )
    }
}

@Composable
private fun PlayerTransportButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    containerColor: Color,
    contentColor: Color
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.94f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 600f)
        },
        label = "player_transport_scale"
    )
    val shape = RoundedCornerShape(32.dp)
    Surface(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, shape, spotColor = containerColor),
        shape = shape,
        color = containerColor
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interaction,
                    indication = LocalIndication.current,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            if (label == null) {
                Icon(
                    icon,
                    contentDescription,
                    tint = contentColor,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = contentColor, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        color = contentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Metrolist'teki bağlı düğme grubu: karıştır, tekrar, sözler, sıra ve
 * uyku zamanlayıcısı tek bir hap içinde.
 */
@Composable
internal fun PlayerConnectedBar(
    isShuffle: Boolean,
    repeatMode: Int,
    isLyricsActive: Boolean,
    isSleepTimerAvailable: Boolean,
    strings: Strings,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onLyrics: () -> Unit,
    onQueue: () -> Unit,
    onSleepTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.07f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConnectedAction(
                icon = Icons.Rounded.Shuffle,
                contentDescription = strings.get("shuffle"),
                active = isShuffle,
                onClick = onShuffle,
                modifier = Modifier.weight(1f)
            )
            ConnectedDivider()
            ConnectedAction(
                icon = if (repeatMode == 2) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = strings.get("repeat"),
                active = repeatMode > 0,
                onClick = onRepeat,
                modifier = Modifier.weight(1f)
            )
            ConnectedDivider()
            ConnectedAction(
                icon = Icons.Rounded.Subtitles,
                contentDescription = strings.get("lyrics"),
                active = isLyricsActive,
                onClick = onLyrics,
                modifier = Modifier.weight(1f)
            )
            ConnectedDivider()
            ConnectedAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = strings.get("queue"),
                active = false,
                onClick = onQueue,
                modifier = Modifier.weight(1f)
            )
            if (isSleepTimerAvailable) {
                ConnectedDivider()
                ConnectedAction(
                    icon = Icons.Rounded.Nightlight,
                    contentDescription = strings.get("sleep_timer"),
                    active = false,
                    onClick = onSleepTimer,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun ConnectedAction(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reducedMotion = LocalReducedMotion.current
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion) 0.84f else 1f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = 700f)
        },
        label = "player_connected_scale"
    )
    val tint = if (active) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.White.copy(alpha = 0.74f)
    }
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .then(
                    if (active) {
                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun ConnectedDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(18.dp)
            .background(Color.White.copy(alpha = 0.10f))
    )
}

/** Menüden açılan ses düzeyi paneli. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerVolumePanel(
    volume: Int,
    maxVolume: Int,
    strings: Strings,
    onVolumeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeMax = maxVolume.coerceAtLeast(1)
    val volumeLabel = strings.get("volume")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (volume == 0) {
                    Icons.AutoMirrored.Rounded.VolumeOff
                } else {
                    Icons.AutoMirrored.Rounded.VolumeUp
                },
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(10.dp))
            Slider(
                value = volume.toFloat(),
                onValueChange = { onVolumeChange(it.toInt()) },
                valueRange = 0f..safeMax.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.18f)
                ),
                thumb = {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                },
                track = { sliderState ->
                    SliderDefaults.Track(
                        sliderState = sliderState,
                        colors = SliderDefaults.colors(
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.18f),
                            activeTickColor = Color.Transparent,
                            inactiveTickColor = Color.Transparent
                        ),
                        drawStopIndicator = {}
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .semantics { contentDescription = volumeLabel }
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${volume * 100 / safeMax}%",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
