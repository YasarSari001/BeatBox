package com.blackstack.beatbox.i18n
import android.content.Context
import com.blackstack.beatbox.AppLanguage

/**
 * Kullanıcının ayarlarda seçtiği dili okur.
 *
 * Tercih `BeatBoxSettings` içinde [AppLanguage] adıyla saklanır. Kayıt yoksa
 * veya bozuksa uygulamanın varsayılan dili olan Türkçeye döner.
 */
fun appLanguageOf(context: Context): AppLanguage {
    val stored = context.getSharedPreferences("BeatBoxSettings", Context.MODE_PRIVATE)
        .getString("app_lang", AppLanguage.TR.name)
    return try {
        AppLanguage.valueOf(stored ?: AppLanguage.TR.name)
    } catch (_: Exception) {
        AppLanguage.TR
    }
}
