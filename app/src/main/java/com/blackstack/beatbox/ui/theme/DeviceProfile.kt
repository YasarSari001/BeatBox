package com.blackstack.beatbox.ui.theme

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Pahalı görsel efektlerin (tam ekran blur, sonsuz sürüklenme, yoğun
 * visualizer) çalışıp çalışmayacağını belirler.
 *
 * Kullanıcının "performans modu" seçimi veya Android'in düşük RAM işareti
 * yeterlidir; ayrıca bellek ölçümü yapılmaz. Değer cihaz başına bir kez
 * okunur, ayar değişikliği uygulama yeniden başlayınca etkin olur.
 */
@Composable
fun rememberPerformanceMode(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        prefs.getBoolean("performance_mode", false) || isLowRamDevice(context)
    }
}

private fun isLowRamDevice(context: Context): Boolean = try {
    (context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager)
        ?.isLowRamDevice == true
} catch (_: Exception) {
    false
}
