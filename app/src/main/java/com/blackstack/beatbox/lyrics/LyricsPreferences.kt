/*
 * BeatBox Project (C) 2026
 *
 * Metrolist'in şarkı sözü tercihlerinden (PreferredLyricsProviderKey,
 * LyricsProviderOrderKey, LyricsTextSizeKey, LyricsGlowEffectKey, LyricsClickKey)
 * uyarlanmıştır. Kaynak: Metrolist (GPL-3.0).
 */

package com.blackstack.beatbox.lyrics

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Şarkı sözü ekranının ve sağlayıcı zincirinin kalıcı tercihleri.
 *
 * @property enabledProviders Aranacak sağlayıcı adları (boş olamaz)
 * @property textSizeSp Şarkı sözü metin boyutu
 * @property glowEnabled Aktif satırda yumuşak ışıma
 * @property clickToSeek Satıra dokunarak o ana atlama
 */
data class LyricsPreferences(
    val enabledProviders: Set<String>,
    val textSizeSp: Float,
    val glowEnabled: Boolean,
    val clickToSeek: Boolean,
) {
    companion object {
        const val PREFS_NAME = "BeatBoxSettings"
        private const val KEY_ENABLED_PROVIDERS = "lyrics_enabled_providers"
        private const val KEY_TEXT_SIZE = "lyrics_text_size"
        private const val KEY_GLOW = "lyrics_glow"
        private const val KEY_CLICK_SEEK = "lyrics_click_seek"

        /**
         * MultiProviderLyricsClient'taki kaynak adlarıyla birebir aynı olmalı.
         * Hepsi anahtarsız çalışır; API anahtarı isteyen sağlayıcılar
         * (BetterLyrics, Musixmatch) kaldırıldı.
         */
        val PROVIDERS = listOf(
            "LRCLIB",
            "lrc.red",
            "Unison",
            "KuGou",
            "Lyrics.ovh",
            "Netease Cloud Music",
        )

        val DEFAULT_ENABLED: Set<String> = PROVIDERS.toSet()

        const val MIN_TEXT_SIZE = 16f
        const val MAX_TEXT_SIZE = 34f
        const val DEFAULT_TEXT_SIZE = 24f

        fun load(prefs: SharedPreferences): LyricsPreferences {
            val stored = prefs.getString(KEY_ENABLED_PROVIDERS, null)
            val enabled = stored
                ?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() && it in PROVIDERS }
                ?.toSet()
                ?.takeIf { it.isNotEmpty() }
                ?: DEFAULT_ENABLED
            return LyricsPreferences(
                enabledProviders = enabled,
                textSizeSp = prefs.getFloat(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
                    .coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE),
                glowEnabled = prefs.getBoolean(KEY_GLOW, true),
                clickToSeek = prefs.getBoolean(KEY_CLICK_SEEK, true),
            )
        }

        fun load(context: Context): LyricsPreferences =
            load(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

        fun saveProviders(prefs: SharedPreferences, providers: Set<String>) {
            if (providers.isEmpty()) return
            prefs.edit { putString(KEY_ENABLED_PROVIDERS, providers.joinToString(",")) }
        }

        fun saveTextSize(prefs: SharedPreferences, sizeSp: Float) {
            prefs.edit { putFloat(KEY_TEXT_SIZE, sizeSp.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)) }
        }

        fun saveGlow(prefs: SharedPreferences, enabled: Boolean) {
            prefs.edit { putBoolean(KEY_GLOW, enabled) }
        }

        fun saveClickToSeek(prefs: SharedPreferences, enabled: Boolean) {
            prefs.edit { putBoolean(KEY_CLICK_SEEK, enabled) }
        }
    }
}
