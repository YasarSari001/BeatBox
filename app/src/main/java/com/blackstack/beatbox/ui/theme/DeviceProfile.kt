package com.blackstack.beatbox.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.blackstack.beatbox.performance.DevicePerformance

/**
 * Pahalı görsel efektlerin (tam ekran blur, sürüklenen aurora, yoğun
 * visualizer, tüm ağacı yeniden çizen renk animasyonları) çalışıp
 * çalışmayacağını belirler.
 *
 * Karar iki kaynaktan gelir: kullanıcının performans modu seçimi ve cihazın
 * donanım sınıfı (RAM, çekirdek, Android sürümü). Karar süreç içinde tutulur
 * ve gözlemlenebilir olduğundan ayar değişikliği yeniden başlatma
 * gerektirmeden uygulanır.
 */
object DeviceProfile {
    internal var overrideState by mutableStateOf<Boolean?>(null)
        private set
    private var initialized = false

    fun initialize(context: Context) {
        if (initialized) return
        val prefs = context.getSharedPreferences(DevicePerformance.PREFS_NAME, Context.MODE_PRIVATE)
        overrideState = DevicePerformance.userOverride(prefs)
        initialized = true
    }

    /** Etkin performans modu; kullanıcı seçimi yoksa cihaz sınıfına göre. */
    fun isReduced(context: Context): Boolean {
        initialize(context)
        return overrideState ?: DevicePerformance.autoReduce(context)
    }

    /** Cihazın kendi sınıfından dolayı kısılmış olup olmadığı. */
    fun isAutoReduced(context: Context): Boolean = DevicePerformance.autoReduce(context)

    /**
     * Kullanıcının açık seçimi; `null` otomatik moda döner.
     * Değişiklik, [rememberPerformanceMode] okuyan tüm bileşenleri anında
     * yeniden compose eder.
     */
    fun setEnabled(context: Context, enabled: Boolean?) {
        val prefs = context.getSharedPreferences(DevicePerformance.PREFS_NAME, Context.MODE_PRIVATE)
        DevicePerformance.setOverride(prefs, enabled)
        overrideState = enabled
        initialized = true
    }
}

@Composable
fun rememberPerformanceMode(): Boolean {
    val context = LocalContext.current
    DeviceProfile.initialize(context)
    // overrideState okuması burada olduğu için ayar değişince recompose olur.
    val override = DeviceProfile.overrideState
    return override ?: DevicePerformance.autoReduce(context)
}
