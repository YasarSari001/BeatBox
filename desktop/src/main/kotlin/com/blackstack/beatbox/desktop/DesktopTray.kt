package com.blackstack.beatbox.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.rememberTrayState
import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.Strings
import java.awt.Color as AwtColor
import java.awt.Font
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage

/**
 * Sistem tepsisi ile pencere arasında paylaşılan durum.
 *
 * Tepsi menüsü [application] kapsamında, oynatma durumu ise pencere içinde
 * yaşadığı için bu küçük köprü gerekir.
 */
class TrayController {
    var strings by mutableStateOf(Strings(AppLanguage.TR))
    var isPlaying by mutableStateOf(false)
    var title by mutableStateOf<String?>(null)
    /** Metin alanı veya pencere açıkken düz klavye kısayollarını engeller. */
    var shortcutsBlocked by mutableStateOf(false)
    var onTogglePlay: () -> Unit = {}
    var onNext: () -> Unit = {}
    var onPrevious: () -> Unit = {}
}

/**
 * BeatBox sistem tepsisi simgesi ve menüsü.
 *
 * Menü: pencereyi göster, çal/duraklat, önceki/sonraki, çıkış.
 */
@Composable
fun ApplicationScope.BeatBoxTray(
    controller: TrayController,
    onShow: () -> Unit,
    onExit: () -> Unit,
) {
    val trayState = rememberTrayState()
    val painter = remember { trayIconPainter() }
    val strings = controller.strings
    val tooltip = buildString {
        append("BeatBox")
        controller.title?.takeIf { it.isNotBlank() }?.let { append(" — ").append(it) }
    }

    Tray(
        icon = painter,
        state = trayState,
        onAction = onShow,
        tooltip = tooltip,
        menu = {
            Item(strings.dt("tray_show"), onClick = onShow)
            Separator()
            Item(
                text = strings.get(if (controller.isPlaying) "pause" else "play"),
                onClick = { controller.onTogglePlay() },
            )
            Item(strings.get("previous"), onClick = { controller.onPrevious() })
            Item(strings.get("next"), onClick = { controller.onNext() })
            Separator()
            Item(strings.dt("tray_exit"), onClick = onExit)
        },
    )
}

/**
 * 32×32 mor-mavi degrade zemin üzerine beyaz nota çizen tepsi simgesi.
 * Ayrı bir görsel dosyası gerektirmez.
 */
private fun trayIconPainter(): BitmapPainter {
    val size = 32
    val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
    val g = image.createGraphics()
    try {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

        val shape = RoundRectangle2D.Float(1f, 1f, size - 2f, size - 2f, 11f, 11f)
        g.paint = java.awt.GradientPaint(
            0f, 0f, AwtColor(0x8B, 0x5C, 0xF6),
            size.toFloat(), size.toFloat(), AwtColor(0x22, 0xD3, 0xEE),
        )
        g.fill(shape)

        g.color = AwtColor.WHITE
        g.font = Font("Segoe UI Symbol", Font.BOLD, 21)
        val text = "♪"
        val metrics = g.fontMetrics
        val x = (size - metrics.stringWidth(text)) / 2
        val y = (size - metrics.height) / 2 + metrics.ascent
        g.drawString(text, x, y)
    } finally {
        g.dispose()
    }
    return BitmapPainter(image.toComposeImageBitmap())
}
