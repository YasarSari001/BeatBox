package com.blackstack.beatbox.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.blackstack.beatbox.Strings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

/**
 * Aşağı çekerek yenileme (pull-to-refresh) sarmalayıcısı.
 *
 * İçeriğin en üstünden aşağı çekildiğinde [onRefresh] çağrılır.
 * [isRefreshing] true olduğunda üstte bir ilerleme göstergesi görünür.
 */
@Composable
fun PullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    strings: Strings,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val threshold = with(density) { 60.dp.toPx() }
    var dragStartY by remember { mutableStateOf(Float.NaN) }
    var dragOffset by remember { mutableStateOf(0f) }
    var canRefresh by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // İçerik
        content()

        // Yenileme ilerleme göstergesi (en üstte, içerik üzerine)
        if (isRefreshing || dragOffset > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (dragOffset / 2).dp)
                    .width(48.dp)
                    .height(48.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Aşağı çekme algılama (en üstteki boş alanda)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .align(Alignment.TopStart)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = { offset ->
                            dragStartY = offset.y
                            canRefresh = false
                        },
                        onVerticalDrag = { change, _ ->
                            val currentY = change.position.y
                            if (dragStartY.isNaN()) {
                                dragStartY = currentY
                            }
                            val delta = currentY - dragStartY
                            if (delta.compareTo(0f) > 0 && currentY.compareTo(80f) <= 0) {
                                dragOffset = delta.coerceAtMost(threshold.toFloat())
                                if (dragOffset.compareTo(threshold * 0.5f) > 0) {
                                    canRefresh = true
                                }
                            } else {
                                dragOffset = 0f
                            }
                        },
                        onDragEnd = {
                            dragStartY = Float.NaN
                            if (canRefresh && dragOffset.compareTo(threshold * 0.5f) > 0) {
                                onRefresh()
                            }
                            dragOffset = 0f
                            canRefresh = false
                        }
                    )
                }
        )

        // "Yenilemek için aşağı çekin" ipucu
        if (dragOffset > 0 && !isRefreshing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (dragOffset + 50).dp)
            ) {
                Text(
                    text = strings.get("refresh"),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                )
            }
        }
    }
}
