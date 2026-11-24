package com.blackstack.beatbox.performance

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build

/**
 * Zayıf cihaz tespiti ve performans modu kararı.
 *
 * Compose'dan bağımsızdır; hem UI hem de arka plan servisi aynı kararı
 * paylaşır. Otomatik tespit süreç başına bir kez hesaplanır.
 */
object DevicePerformance {
    const val PREFS_NAME = "BeatBoxSettings"

    /** Kullanıcının açık seçimi. Yoksa otomatik tespit geçerlidir. */
    private const val KEY_OVERRIDE = "performance_mode_override"

    /** Eski sürümlerdeki tek anahtar; yalnızca manuel açma olarak yorumlanır. */
    private const val KEY_LEGACY = "performance_mode"

    /** 2 GB ve altı cihazlar her zaman kısılmış moda geçer. */
    private const val LOW_RAM_MB = 2048L

    /** 3 GB + en fazla 4 çekirdek: eski ama kullanılabilir donanım. */
    private const val MID_RAM_MB = 3072L

    /** Android 8.x döneminde 4 GB bile ağır Compose efektlerinde zorlanır. */
    private const val LEGACY_SDK = 27

    @Volatile
    private var autoReduced: Boolean? = null

    /**
     * Saf karar fonksiyonu; Android çalışma zamanına ihtiyaç duymaz ve
     * birim testi vardır.
     */
    fun shouldReduceFor(
        isLowRamDevice: Boolean,
        totalRamMb: Long,
        cpuCores: Int,
        sdkInt: Int,
    ): Boolean {
        if (isLowRamDevice) return true
        if (cpuCores in 1..2) return true
        // MemoryInfo okuyamadığımızda (0) RAM'e göre karar vermeyiz.
        if (totalRamMb in 1..LOW_RAM_MB) return true
        if (totalRamMb in 1..MID_RAM_MB && cpuCores <= 4) return true
        if (sdkInt <= LEGACY_SDK && totalRamMb in 1..4096L) return true
        return false
    }

    /** Otomatik tespit sonucu; cihaz donanımına göre süreç başına bir kez. */
    fun autoReduce(context: Context): Boolean = autoReduced ?: detect(context).also { autoReduced = it }

    /** Kullanıcının açık seçimi; null ise otomatik mod. */
    fun userOverride(prefs: SharedPreferences): Boolean? = when {
        prefs.contains(KEY_OVERRIDE) -> prefs.getBoolean(KEY_OVERRIDE, false)
        // Eski sürüm yalnızca "açık" durumunu kaydediyordu; saygı göster.
        prefs.getBoolean(KEY_LEGACY, false) -> true
        else -> null
    }

    fun isEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return userOverride(prefs) ?: autoReduce(context)
    }

    fun setOverride(prefs: SharedPreferences, enabled: Boolean?) {
        prefs.edit().apply {
            if (enabled == null) remove(KEY_OVERRIDE) else putBoolean(KEY_OVERRIDE, enabled)
            remove(KEY_LEGACY)
        }.apply()
    }

    private fun detect(context: Context): Boolean {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                ?: return false
        val totalRamMb = try {
            val info = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            info.totalMem / (1024L * 1024L)
        } catch (_: Exception) {
            0L
        }
        return shouldReduceFor(
            isLowRamDevice = activityManager.isLowRamDevice,
            totalRamMb = totalRamMb,
            cpuCores = Runtime.getRuntime().availableProcessors(),
            sdkInt = Build.VERSION.SDK_INT,
        )
    }
}
