package com.blackstack.beatbox.auth

import android.content.Context
import com.blackstack.beatbox.AuthProvider
import com.blackstack.beatbox.UserProfile
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.tasks.await

/**
 * Giriş işleminin sonucu.
 *
 * [Message] yalnızca kullanıcıya gösterilecek bilgi mesajları için kullanılır
 * (ör. şifre sıfırlama e-postası gönderildi).
 */
sealed interface AuthResult {
    data class Success(val user: UserProfile) : AuthResult
    data class Message(val message: String) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * Kimlik doğrulama sözleşmesi.
 *
 * UI katmanı yalnızca bu arayüze bağlıdır; Firebase yapılandırılmadığında
 * [FirebaseAuthRepository.isConfigured] false döner ve misafir girişi çalışmaya
 * devam eder.
 */
interface AuthRepository {
    val isConfigured: Boolean

    fun currentUser(): UserProfile?

    suspend fun signInWithEmail(email: String, password: String): AuthResult

    suspend fun registerWithEmail(
        displayName: String,
        email: String,
        password: String
    ): AuthResult

    suspend fun signInWithGoogle(idToken: String): AuthResult

    suspend fun sendPasswordReset(email: String): AuthResult

    fun signOut()
}

/**
 * Firebase Authentication uygulaması.
 *
 * Firebase yalnızca `google-services.json` eklendiğinde başlatılır. Dosya
 * yokken [firebaseAuth] null kalır; böylece uygulama çökmeden misafir moduna
 * düşer.
 */
class FirebaseAuthRepository(context: Context) : AuthRepository {

    private val appContext = context.applicationContext

    private val firebaseAuth: FirebaseAuth? = runCatching {
        if (FirebaseApp.getApps(appContext).isEmpty()) {
            null
        } else {
            FirebaseAuth.getInstance()
        }
    }.getOrNull()

    override val isConfigured: Boolean
        get() = firebaseAuth != null

    override fun currentUser(): UserProfile? =
        firebaseAuth?.currentUser?.toProfile()

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.toProfile()?.let(AuthResult::Success)
                ?: AuthResult.Failure("Oturum bilgisi alınamadı.")
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage())
        }
    }

    override suspend fun registerWithEmail(
        displayName: String,
        email: String,
        password: String
    ): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
                ?: return AuthResult.Failure("Hesap oluşturuldu ancak oturum bilgisi alınamadı.")

            val name = displayName.trim().ifBlank { email.substringBefore("@") }
            runCatching {
                user.updateProfile(
                    UserProfileChangeRequest.Builder()
                        .setDisplayName(name)
                        .build()
                ).await()
            }
            // Doğrulama e-postası gönderilemezse kayıt yine de başarılı sayılır.
            runCatching { user.sendEmailVerification().await() }

            AuthResult.Success(user.toProfile().copy(displayName = name))
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage())
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.toProfile()?.let(AuthResult::Success)
                ?: AuthResult.Failure("Google oturumu açılamadı.")
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage())
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Message("Şifre sıfırlama bağlantısı e-posta adresinize gönderildi.")
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage())
        }
    }

    override fun signOut() {
        runCatching { firebaseAuth?.signOut() }
    }

    private fun notConfigured() = AuthResult.Failure(
        "Firebase yapılandırılmadı. app/google-services.json dosyasını ekleyip yeniden derleyin."
    )
}

/** Firebase oturumunu kapatır; yapılandırma yoksa sessizce döner. */
fun signOutFirebase(context: Context) {
    runCatching {
        if (FirebaseApp.getApps(context.applicationContext).isNotEmpty()) {
            FirebaseAuth.getInstance().signOut()
        }
    }
}

private fun FirebaseUser.toProfile(): UserProfile {
    val provider = when {
        providerData.any { it.providerId == GoogleAuthProvider.PROVIDER_ID } -> AuthProvider.GOOGLE
        providerData.any { it.providerId == "phone" } -> AuthProvider.PHONE
        providerData.any { it.providerId == "password" } -> AuthProvider.EMAIL
        else -> AuthProvider.EMAIL
    }
    val mail = email.orEmpty()
    val phone = phoneNumber.orEmpty()
    val name = displayName?.takeIf { it.isNotBlank() }
        ?: mail.substringBefore("@").takeIf { it.isNotBlank() }
        ?: phone.takeIf { it.isNotBlank() }
        ?: "BeatBox Dinleyicisi"

    return UserProfile(
        id = uid,
        displayName = name,
        email = mail,
        phoneNumber = phone,
        isLoggedIn = true,
        authProvider = provider,
        joinedDate = metadata?.creationTimestamp ?: System.currentTimeMillis()
    )
}

private fun Throwable.toAuthMessage(): String =
    if (this is FirebaseAuthException) authErrorMessage(errorCode) else GENERIC_AUTH_ERROR

/**
 * Firebase hata kodlarını kullanıcıya gösterilebilir Türkçe mesajlara çevirir.
 * Bilinmeyen kodlarda İngilizce teknik metin göstermek yerine genel mesaj döner.
 */
internal fun authErrorMessage(errorCode: String?): String = when (errorCode) {
    "ERROR_INVALID_EMAIL" -> "Geçerli bir e-posta adresi girin."
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> "E-posta veya şifre hatalı."
    "ERROR_USER_NOT_FOUND" -> "Bu e-posta ile kayıtlı bir hesap bulunamadı."
    "ERROR_EMAIL_ALREADY_IN_USE" -> "Bu e-posta adresi zaten kullanılıyor."
    "ERROR_WEAK_PASSWORD" -> "Şifre en az 6 karakter olmalı."
    "ERROR_MISSING_EMAIL" -> "E-posta adresi gerekli."
    "ERROR_MISSING_PASSWORD" -> "Şifre gerekli."
    "ERROR_TOO_MANY_REQUESTS" -> "Çok fazla deneme yapıldı. Lütfen biraz sonra tekrar deneyin."
    "ERROR_NETWORK_REQUEST_FAILED" -> "İnternet bağlantınızı kontrol edin."
    "ERROR_USER_DISABLED" -> "Bu hesap devre dışı bırakılmış."
    "ERROR_OPERATION_NOT_ALLOWED" ->
        "E-posta/şifre girişi Firebase konsolunda etkin değil."
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
        "Bu e-posta farklı bir giriş yöntemiyle kayıtlı."
    "ERROR_REQUIRES_RECENT_LOGIN" -> "Bu işlem için yeniden giriş yapmanız gerekiyor."
    "ERROR_INVALID_ACTION_CODE" -> "Bağlantının süresi dolmuş veya geçersiz."
    else -> GENERIC_AUTH_ERROR
}

private const val GENERIC_AUTH_ERROR = "Giriş yapılamadı. Lütfen tekrar deneyin."
