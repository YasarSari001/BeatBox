package com.blackstack.beatbox.auth

import android.app.Activity
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** Google ile giriş denemesinin sonucu. */
sealed interface GoogleSignInResult {
    data class Success(val idToken: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
}

/**
 * Credential Manager üzerinden Google kimlik belirteci alır.
 *
 * Google girişi yalnızca `google-services.json` içinde bir web OAuth istemcisi
 * tanımlıysa kullanılabilir; aksi halde [isAvailable] false döner ve UI bu
 * seçeneği göstermez.
 */
class GoogleSignInClient(private val context: Context) {

    private val credentialManager by lazy { CredentialManager.create(context) }

    val isAvailable: Boolean
        get() = context.googleWebClientId() != null

    suspend fun signIn(): GoogleSignInResult {
        val webClientId = context.googleWebClientId()
            ?: return GoogleSignInResult.Failure(
                "Google girişi için web istemci kimliği bulunamadı."
            )

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(
                GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(false)
                    .build()
            )
            .build()

        return try {
            val response = credentialManager.getCredential(context, request)
            val credential = response.credential
            if (
                credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                GoogleSignInResult.Success(googleCredential.idToken)
            } else {
                GoogleSignInResult.Failure("Google kimlik bilgisi alınamadı.")
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failure(
                "Cihazda kullanılabilir Google hesabı bulunamadı. Lütfen Google hesabı ekleyin."
            )
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure("Google kimlik belirteci okunamadı.")
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(
                e.localizedMessage ?: "Google girişi başarısız. Lütfen tekrar deneyin."
            )
        }
    }
}

/** google-services eklentisinin ürettiği web istemci kimliği. */
@SuppressLint("DiscouragedApi")
internal fun Context.googleWebClientId(): String? {
    val resId = resources.getIdentifier("default_web_client_id", "string", packageName)
    if (resId == 0) return null
    return runCatching { getString(resId) }.getOrNull()?.takeIf { it.isNotBlank() }
}

/** Compose'un verdiği Context'ten Activity'ye ulaşır. */
internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
