package com.blackstack.beatbox.auth

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Giriş formunun yükleme ve mesaj durumunu yönetir.
 *
 * Repository çağrıları [scope] üzerinde çalışır; diyalog kapandığında kapsam
 * iptal edilir. Aynı anda ikinci bir istek gönderilmez.
 */
@Stable
class AuthController(
    private val repository: AuthRepository,
    private val scope: CoroutineScope,
    private val strings: Strings
) {
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var infoMessage by mutableStateOf<String?>(null)
        private set

    val isConfigured: Boolean
        get() = repository.isConfigured

    fun clearMessages() {
        errorMessage = null
        infoMessage = null
    }

    fun reportError(message: String) {
        infoMessage = null
        errorMessage = message
    }

    fun signInWithEmail(
        email: String,
        password: String,
        onSuccess: (UserProfile) -> Unit
    ) = submit(onSuccess) {
        repository.signInWithEmail(email.trim(), password)
    }

    fun registerWithEmail(
        displayName: String,
        email: String,
        password: String,
        onSuccess: (UserProfile) -> Unit
    ) = submit(onSuccess) {
        repository.registerWithEmail(displayName.trim(), email.trim(), password)
    }

    fun signInWithGoogle(
        idToken: String,
        onSuccess: (UserProfile) -> Unit
    ) = submit(onSuccess) {
        repository.signInWithGoogle(idToken)
    }

    fun sendPasswordReset(email: String) {
        if (isLoading) return
        clearMessages()
        isLoading = true
        scope.launch {
            when (val result = repository.sendPasswordReset(email.trim())) {
                is AuthResult.Message -> infoMessage = result.message
                is AuthResult.Failure -> errorMessage = result.message
                is AuthResult.Success -> infoMessage = strings.get("action_completed")
            }
            isLoading = false
        }
    }

    private fun submit(
        onSuccess: (UserProfile) -> Unit,
        action: suspend () -> AuthResult
    ) {
        if (isLoading) return
        clearMessages()
        isLoading = true
        scope.launch {
            val result = runCatching { action() }.getOrElse {
                AuthResult.Failure(it.localizedMessage ?: strings.get("auth_generic_error"))
            }
            when (result) {
                is AuthResult.Success -> {
                    isLoading = false
                    onSuccess(result.user)
                }
                is AuthResult.Message -> {
                    isLoading = false
                    infoMessage = result.message
                }
                is AuthResult.Failure -> {
                    isLoading = false
                    errorMessage = result.message
                }
            }
        }
    }
}
