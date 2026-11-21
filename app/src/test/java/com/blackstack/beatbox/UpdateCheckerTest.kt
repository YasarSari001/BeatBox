package com.blackstack.beatbox

import com.blackstack.beatbox.updates.UpdateChecker
import com.blackstack.beatbox.updates.UpdateInfo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * update.json ayrıştırması org.json kullandığı için JVM birim testlerinde
 * çalıştırılamıyor (Android stub'ları); burada yalnızca sürüm karşılaştırması
 * doğrulanır.
 */
class UpdateCheckerTest : StringSpec({

    "sürüm karşılaştırması mevcut sürümden büyükse güncelleme vardır" {
        val current = BuildConfig.VERSION_CODE
        UpdateChecker.isNewer(UpdateInfo(current + 1, "yeni", "https://e/x.apk", "")) shouldBe true
        UpdateChecker.isNewer(UpdateInfo(current, "aynı", "https://e/x.apk", "")) shouldBe false
        UpdateChecker.isNewer(UpdateInfo(current - 1, "eski", "https://e/x.apk", "")) shouldBe false
    }
})
