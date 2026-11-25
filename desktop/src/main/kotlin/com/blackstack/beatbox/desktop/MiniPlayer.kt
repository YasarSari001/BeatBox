package com.blackstack.beatbox.desktop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings

/**
 * Oynatıcı paneli gizlendiğinde görünen küçük oynatma çubuğu.
 *
 * Parça bilgisi, çal/duraklat, sonraki parça ve ilerleme çubuğu içerir;
 * tıklamak tam paneli geri açar.
 */
@Composable
fun MiniPlayer(
    strings: Strings,
    track: Track,
    isPlaying: Boolean,
    position: Double,
    duration: Double,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onShowPanel: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary

    // Çubuk ilk göründüğünde alttan yükselerek belirir; ilerleme de
    // sıçramadan, yumuşak bir geçişle ilerler.
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val appearProgress by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing),
        label = "mini_player_appear",
    )
    val progressFraction by animateFloatAsState(
        targetValue = if (duration > 0.0) (position / duration).toFloat().coerceIn(0f, 1f) else 0f,
        animationSpec = tween(durationMillis = 320),
        label = "mini_player_progress",
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .height(68.dp)
            .graphicsLayer {
                alpha = appearProgress
                translationY = (1f - appearProgress) * 18.dp.toPx()
            }
            .clickable { onShowPanel() },
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(accent, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "♪",
                        color = Color.White,
                        fontSize = 20.sp,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = listOfNotNull(
                            track.artist.takeIf { it.isNotBlank() },
                            formatTime(position),
                        ).joinToString(" · "),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onTogglePlay) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = strings.get(if (isPlaying) "pause" else "play"),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        contentDescription = strings.get("next"),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(onClick = onShowPanel) {
                    Icon(
                        Icons.Rounded.KeyboardArrowUp,
                        contentDescription = strings.dt("show_player"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (duration > 0.0) {
                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .align(Alignment.BottomCenter),
                    color = accent,
                    trackColor = Color.Transparent,
                )
            }
        }
    }
}
