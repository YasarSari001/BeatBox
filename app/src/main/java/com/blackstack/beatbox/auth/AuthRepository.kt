package com.blackstack.beatbox.auth

import android.content.Context
import com.blackstack.beatbox.AuthProvider
import com.blackstack.beatbox.Strings
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
class FirebaseAuthRepository(context: Context, private val strings: Strings) : AuthRepository {

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
        firebaseAuth?.currentUser?.toProfile(strings)

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.toProfile(strings)?.let(AuthResult::Success)
                ?: AuthResult.Failure(strings.get("err_session_not_found"))
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage(strings))
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
                ?: return AuthResult.Failure(strings.get("err_account_no_session"))

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

            AuthResult.Success(user.toProfile(strings).copy(displayName = name))
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage(strings))
        }
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.toProfile(strings)?.let(AuthResult::Success)
                ?: AuthResult.Failure(strings.get("err_google_session"))
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage(strings))
        }
    }

    override suspend fun sendPasswordReset(email: String): AuthResult {
        val auth = firebaseAuth ?: return notConfigured()
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Message(strings.get("reset_email_sent"))
        } catch (e: Exception) {
            AuthResult.Failure(e.toAuthMessage(strings))
        }
    }

    override fun signOut() {
        runCatching { firebaseAuth?.signOut() }
    }

    private fun notConfigured() = AuthResult.Failure(
        strings.get("firebase_not_configured")
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

private fun FirebaseUser.toProfile(strings: Strings): UserProfile {
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
        ?: strings.get("listener")

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

private fun Throwable.toAuthMessage(strings: Strings): String =
    if (this is FirebaseAuthException) authErrorMessage(errorCode, strings)
    else strings.get("auth_generic_error")

/**
 * Firebase hata kodlarını yerelleştirilmiş kullanıcı mesajlarına çevirir.
 * Bilinmeyen kodlarda teknik metin yerine genel mesaj döner.
 */
internal fun authErrorMessage(errorCode: String?, strings: Strings): String = when (errorCode) {
    "ERROR_INVALID_EMAIL" -> strings.get("err_invalid_email")
    "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL" -> strings.get("auth_wrong_password")
    "ERROR_USER_NOT_FOUND" -> strings.get("auth_user_not_found")
    "ERROR_EMAIL_ALREADY_IN_USE" -> strings.get("auth_email_in_use")
    "ERROR_WEAK_PASSWORD" -> strings.get("err_password_min")
    "ERROR_MISSING_EMAIL" -> strings.get("auth_missing_email")
    "ERROR_MISSING_PASSWORD" -> strings.get("auth_missing_password")
    "ERROR_TOO_MANY_REQUESTS" -> strings.get("auth_too_many_requests")
    "ERROR_NETWORK_REQUEST_FAILED" -> strings.get("auth_network")
    "ERROR_USER_DISABLED" -> strings.get("auth_user_disabled")
    "ERROR_OPERATION_NOT_ALLOWED" -> strings.get("auth_operation_not_allowed")
    "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
        strings.get("auth_account_diff_credential")
    "ERROR_REQUIRES_RECENT_LOGIN" -> strings.get("auth_requires_recent_login")
    "ERROR_INVALID_ACTION_CODE" -> strings.get("auth_invalid_action_code")
    else -> strings.get("auth_generic_error")
}
