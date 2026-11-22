package com.blackstack.beatbox.ui.theme

import android.app.ActivityManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Cihazın görsel efekt bütçesini belirleyen hafif profil.
 *
 * Düşük RAM'li cihazlarda tam ekran blur, sürekli arka plan sürüklenmesi ve
 * çok sayıda eşzamanlı animasyon kare düşmesine yol açar. Bu profil, bu
 * efektleri otomatik olarak sadeleştirmek için tek bir karar noktası sağlar.
 */
object DeviceProfile {
    @Volatile
    private var cachedLowEnd: Boolean? = null

    /** Android'in low-RAM işareti veya ~3,5 GB altı toplam bellek. */
    fun isLowEnd(context: Context): Boolean {
        cachedLowEnd?.let { return it }
        val result = try {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memory = ActivityManager.MemoryInfo()
            manager?.getMemoryInfo(memory)
            val totalGb = memory.totalMem / (1024.0 * 1024.0 * 1024.0)
            manager?.isLowRamDevice == true || totalGb < 3.5
        } catch (_: Exception) {
            false
        }
        cachedLowEnd = result
        return result
    }
}

/**
 * Kullanıcı ayarlarındaki "performans modu" veya düşük donanım profili.
 *
 * `true` iken pahalı efektler (blur, sonsuz animasyon, yoğun visualizer)
 * atlanır; içerik ve işlevler aynı kalır.
 */
@Composable
fun rememberPerformanceMode(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val prefs = context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        prefs.getBoolean("performance_mode", false) || DeviceProfile.isLowEnd(context)
    }
}
