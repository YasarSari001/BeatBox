package com.blackstack.beatbox

import com.blackstack.beatbox.performance.DevicePerformance
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Zayıf cihaz sınıflandırmasının birim testleri.
 *
 * Karar mantığı saf bir fonksiyonda tutulur; Android çalışma zamanı olmadan
 * test edilebilir. Amaç, "tost makinesi" sınıfı cihazların her koşulda
 * performans moduna geçmesi ve güçlü cihazların gereksiz yere kısılmaması.
 */
class DevicePerformanceTest : StringSpec({

    "low ram devices always reduce" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = true,
            totalRamMb = 8192,
            cpuCores = 8,
            sdkInt = 34
        ) shouldBe true
    }

    "one or two cores always reduce" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 8192,
            cpuCores = 2,
            sdkInt = 34
        ) shouldBe true
    }

    "2 GB and below reduces regardless of cores" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 2048,
            cpuCores = 8,
            sdkInt = 34
        ) shouldBe true
    }

    "3 GB with four cores reduces" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 3072,
            cpuCores = 4,
            sdkInt = 34
        ) shouldBe true
    }

    "3 GB with six cores keeps full quality" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 3072,
            cpuCores = 6,
            sdkInt = 34
        ) shouldBe false
    }

    "Android 8 device with 4 GB reduces" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 4096,
            cpuCores = 8,
            sdkInt = 27
        ) shouldBe true
    }

    "Android 8 device with more than 4 GB keeps full quality" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 6144,
            cpuCores = 8,
            sdkInt = 27
        ) shouldBe false
    }

    "modern mid-range device keeps full quality" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 6144,
            cpuCores = 8,
            sdkInt = 34
        ) shouldBe false
    }

    "unknown memory size is not treated as low ram" {
        DevicePerformance.shouldReduceFor(
            isLowRamDevice = false,
            totalRamMb = 0,
            cpuCores = 8,
            sdkInt = 34
        ) shouldBe false
    }
})
