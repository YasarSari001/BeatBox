package com.blackstack.beatbox

/**
 * Decode edilecek görselin hedef boyuta göre 2'nin kuvveti örnekleme
 * katsayısını hesaplar. Saf Kotlin olduğu için birim testleri Android
 * çalışma zamanına ihtiyaç duymaz.
 */
internal fun calculateInSampleSize(width: Int, height: Int, targetSize: Int): Int {
    val maxDimension = targetSize.coerceAtLeast(1) * 2
    var sample = 1
    while (width / sample > maxDimension || height / sample > maxDimension) {
        sample *= 2
    }
    return sample
}
