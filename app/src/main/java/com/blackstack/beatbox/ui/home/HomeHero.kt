package com.blackstack.beatbox.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.ui.components.AlbumArtImage
import com.blackstack.beatbox.AudioFile
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.ui.theme.LocalReducedMotion
import kotlinx.coroutines.delay
// ---------------------------------------------------------------------------
// Hero: devam eden çalma veya keşfet daveti
// ---------------------------------------------------------------------------

@Composable
internal fun HomeHero(
    currentAudio: AudioFile?,
    isPlaying: Boolean,
    isFavorite: Boolean,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    onOpenPlayer: () -> Unit,
    onShuffleAll: () -> Unit,
    songCount: Int,
    strings: Strings
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val shape = RoundedCornerShape(28.dp)
    val haptic = LocalHapticFeedback.current
    val reducedMotion = LocalReducedMotion.current
    val audio = currentAudio
    val playing = audio != null && isPlaying

    var rawProgress by remember(audio?.id) { mutableFloatStateOf(0f) }
    LaunchedEffect(audio?.id, isPlaying) {
        if (audio == null) {
            rawProgress = 0f
            return@LaunchedEffect
        }
        while (true) {
            rawProgress = progress().coerceIn(0f, 1f)
            delay(if (isPlaying) 700 else 2_000)
        }
    }
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (reducedMotion) tween(0) else tween(420),
        label = "home_hero_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(
                elevation = 18.dp,
                shape = shape,
                ambientColor = primary.copy(alpha = 0.42f),
                spotColor = secondary.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        primary.copy(alpha = 0.30f),
                        secondary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.20f),
                        primary.copy(alpha = 0.26f),
                        Color.White.copy(alpha = 0.06f)
                    )
                ),
                shape = shape
            )
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                if (audio != null) onOpenPlayer() else onShuffleAll()
            }
            .padding(16.dp)
    ) {
        // Sağ üstteki yumuşak aurora lekesi, karta derinlik verir.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 34.dp, y = (-40).dp)
                .size(150.dp)
                .background(
                    Brush.radialGradient(
                        listOf(secondary.copy(alpha = 0.30f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(78.dp)
                    .shadow(10.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                if (audio != null) {
                    AlbumArtImage(audio, Modifier.fillMaxSize(), isHQ = true)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        primary.copy(alpha = 0.82f),
                                        secondary.copy(alpha = 0.72f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    text = if (audio != null) {
                        if (playing) strings.get("now_playing_badge") else strings.get("continue_listening")
                    } else {
                        strings.get("welcome")
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp,
                    color = primary.copy(alpha = 0.95f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = audio?.title ?: strings.get("app_name"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isFavorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = secondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = audio?.artist ?: "$songCount ${strings.get("songs")}",
                    fontSize = 12.sp,
                    color = onSurface.copy(alpha = 0.66f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (audio != null) {
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.16f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)
                                .fillMaxHeight()
                                .background(Brush.horizontalGradient(listOf(primary, secondary)))
                        )
                    }
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${strings.get("shuffle")} · ${strings.get("start")}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(primary, secondary.copy(alpha = 0.90f))
                        )
                    )
                    .clearAndSetSemantics {
                        contentDescription = if (playing) strings.get("pause") else strings.get("play")
                        role = Role.Button
                        onClick {
                            if (audio != null) onPlayPause() else onShuffleAll()
                            true
                        }
                    }
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        if (audio != null) onPlayPause() else onShuffleAll()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}
