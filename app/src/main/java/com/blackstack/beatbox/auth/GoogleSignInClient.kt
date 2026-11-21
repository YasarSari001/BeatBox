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
import com.blackstack.beatbox.Strings

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
class GoogleSignInClient(
    private val context: Context,
    private val strings: Strings
) {

    private val credentialManager by lazy { CredentialManager.create(context) }

    val isAvailable: Boolean
        get() = context.googleWebClientId() != null

    suspend fun signIn(): GoogleSignInResult {
        val webClientId = context.googleWebClientId()
            ?: return GoogleSignInResult.Failure(
                strings.get("google_web_client_missing")
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
                GoogleSignInResult.Failure(strings.get("google_credential_missing"))
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleSignInResult.Failure(
                strings.get("google_no_account")
            )
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(strings.get("google_token_unreadable"))
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(
                e.localizedMessage ?: strings.get("google_sign_in_failed")
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
