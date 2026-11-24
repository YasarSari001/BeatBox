package com.blackstack.beatbox.desktop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/*
 * update.json ayrıştırmasının masaüstü tarafı.
 *
 * Ağa çıkmadan yalnızca gövde ayrıştırması sınanır: Android'deki
 * UpdateCheckerTest ile aynı yaklaşım.
 */
class DesktopUpdateTest {

    @Test
    fun `duz json alanlarini okur`() {
        val body = """
            {
              "versionCode": 15,
              "versionName": "3.9.3",
              "apkUrl": "https://e/x.apk",
              "changelog": "BeatBox 3.9.3\n• İlk satır\n• İkinci satır"
            }
        """.trimIndent()

        val update = DesktopUpdateChecker.parseManifest(body)

        assertEquals(15, update?.versionCode)
        assertEquals("3.9.3", update?.versionName)
        assertEquals("BeatBox 3.9.3\n• İlk satır\n• İkinci satır", update?.changelog)
    }

    @Test
    fun `kacis dizileri cozulur`() {
        val body = """{"versionCode": 16, "versionName": "3.9.4", "changelog": "Tırnak: \"x\" \\ ters"}"""

        val update = DesktopUpdateChecker.parseManifest(body)

        assertEquals("Tırnak: \"x\" \\ ters", update?.changelog)
    }

    @Test
    fun `versionCode yoksa null doner`() {
        assertNull(DesktopUpdateChecker.parseManifest("""{"versionName": "3.9.4"}"""))
        assertNull(DesktopUpdateChecker.parseManifest("bozuk json"))
    }

    @Test
    fun `yalnizca daha yeni surum guncelleme sayilir`() {
        val current = DesktopUpdateChecker.VERSION_CODE
        fun update(code: Int) = DesktopUpdate(code, "x", "")

        assertEquals(true, DesktopUpdateChecker.isNewer(update(current + 1)))
        assertEquals(false, DesktopUpdateChecker.isNewer(update(current)))
        assertEquals(false, DesktopUpdateChecker.isNewer(update(current - 1)))
    }

    @Test
    fun `sema disi govdede versionName koda duser`() {
        val update = DesktopUpdateChecker.parseManifest("""{"versionCode": 14}""")

        assertEquals("14", update?.versionName)
        assertEquals("", update?.changelog)
    }
}
