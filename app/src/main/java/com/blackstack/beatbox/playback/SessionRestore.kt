package com.blackstack.beatbox.playback
import android.content.Context
import android.content.SharedPreferences
import com.blackstack.beatbox.auth.FirebaseAuthRepository
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.UserProfile
import com.blackstack.beatbox.ui.components.loadUserProfile
import com.blackstack.beatbox.ui.components.saveUserProfile

/**
 * Uygulama açılırken yerel profili Firebase oturumuyla birleştirir.
 *
 * Firebase oturumu cihazda kalıcıdır; `currentUser` süreç başlangıcında
 * senkron olarak okunabilir. Aynı e-posta ile daha önce düzenlenen bio,
 * avatar ve premium tercihleri korunur. Firebase yapılandırılmadığında
 * yalnızca yerel profil döner.
 */
fun restoreSession(prefs: SharedPreferences, context: Context, strings: Strings): UserProfile {
    val local = loadUserProfile(prefs, strings)
    val remote = FirebaseAuthRepository(context, strings).currentUser() ?: return local

    val restored = mergeSessionProfile(local, remote)
    saveUserProfile(prefs, restored)
    return restored
}

/**
 * Yerel tercihleri uzak oturumla birleştirir.
 *
 * Bio, avatar ve premium durumu yalnızca aynı e-posta hesabında korunur;
 * farklı bir hesapla giriş yapıldığında önceki hesabın tercihleri taşınmaz.
 */
internal fun mergeSessionProfile(local: UserProfile, remote: UserProfile): UserProfile {
    val sameAccount = remote.email.isNotBlank() &&
        remote.email.equals(local.email, ignoreCase = true)
    return remote.copy(
        bio = if (sameAccount) local.bio else remote.bio,
        avatarId = if (sameAccount) local.avatarId else remote.avatarId,
        isPremium = if (sameAccount) local.isPremium else remote.isPremium
    )
}
