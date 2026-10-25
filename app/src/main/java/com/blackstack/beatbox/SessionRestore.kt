package com.blackstack.beatbox

import android.content.Context
import android.content.SharedPreferences
import com.blackstack.beatbox.auth.FirebaseAuthRepository

/**
 * Uygulama açılırken yerel profili Firebase oturumuyla birleştirir.
 *
 * Firebase oturumu cihazda kalıcıdır; `currentUser` süreç başlangıcında
 * senkron olarak okunabilir. Aynı e-posta ile daha önce düzenlenen bio,
 * avatar ve premium tercihleri korunur. Firebase yapılandırılmadığında
 * yalnızca yerel profil döner.
 */
fun restoreSession(prefs: SharedPreferences, context: Context): UserProfile {
    val local = loadUserProfile(prefs)
    val remote = FirebaseAuthRepository(context).currentUser() ?: return local

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
