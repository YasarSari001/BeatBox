package com.blackstack.beatbox.auth

import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.AuthProvider
import com.blackstack.beatbox.Strings
import com.blackstack.beatbox.UserProfile
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * Giriş formunun durum makinesini doğrular: yükleme göstergesi, hata
 * mesajları, e-posta kırpma ve aynı anda ikinci istek engeli.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthControllerTest : StringSpec({

    val strings = Strings(AppLanguage.TR)

    val profile = UserProfile(
        id = "uid-1",
        displayName = "Deniz",
        email = "deniz@example.com",
        isLoggedIn = true,
        authProvider = AuthProvider.EMAIL
    )

    "başarılı giriş kullanıcıyı iletir ve yüklemeyi bitirir" {
        runTest {
            val repository = FakeAuthRepository(AuthResult.Success(profile))
            val controller = AuthController(repository, this, strings)
            var received: UserProfile? = null

            controller.signInWithEmail("  deniz@example.com  ", "123456") { received = it }
            advanceUntilIdle()

            received shouldBe profile
            controller.isLoading shouldBe false
            controller.errorMessage.shouldBeNull()
            repository.lastEmail shouldBe "deniz@example.com"
        }
    }

    "başarısız giriş hata mesajını gösterir" {
        runTest {
            val repository = FakeAuthRepository(AuthResult.Failure("E-posta veya şifre hatalı."))
            val controller = AuthController(repository, this, strings)
            var received: UserProfile? = null

            controller.signInWithEmail("deniz@example.com", "yanlis") { received = it }
            advanceUntilIdle()

            received.shouldBeNull()
            controller.errorMessage shouldBe "E-posta veya şifre hatalı."
            controller.isLoading shouldBe false
        }
    }

    "istek sürerken ikinci giriş denemesi yok sayılır" {
        runTest {
            val repository = GatedAuthRepository()
            val controller = AuthController(repository, this, strings)
            var successCount = 0

            controller.signInWithEmail("a@b.com", "123456") { successCount++ }
            advanceUntilIdle()
            controller.signInWithEmail("a@b.com", "123456") { successCount++ }
            advanceUntilIdle()

            repository.calls shouldBe 1
            controller.isLoading shouldBe true

            repository.gate.complete(AuthResult.Success(profile))
            advanceUntilIdle()

            successCount shouldBe 1
            controller.isLoading shouldBe false
        }
    }

    "kayıt sırasında ad ve e-posta kırpılır" {
        runTest {
            val repository = FakeAuthRepository(AuthResult.Success(profile))
            val controller = AuthController(repository, this, strings)

            controller.registerWithEmail("  Deniz  ", " deniz@example.com ", "123456") {}
            advanceUntilIdle()

            repository.lastDisplayName shouldBe "Deniz"
            repository.lastEmail shouldBe "deniz@example.com"
        }
    }

    "şifre sıfırlama bilgi mesajı üretir" {
        runTest {
            val repository = FakeAuthRepository(
                AuthResult.Message("Şifre sıfırlama bağlantısı gönderildi.")
            )
            val controller = AuthController(repository, this, strings)

            controller.sendPasswordReset(" deniz@example.com ")
            advanceUntilIdle()

            repository.lastEmail shouldBe "deniz@example.com"
            controller.infoMessage shouldBe "Şifre sıfırlama bağlantısı gönderildi."
            controller.errorMessage.shouldBeNull()
            controller.isLoading shouldBe false
        }
    }

    "repository istisnası hata mesajına dönüşür" {
        runTest {
            val controller = AuthController(ThrowingAuthRepository(), this, strings)

            controller.signInWithEmail("a@b.com", "123456") {}
            advanceUntilIdle()

            controller.errorMessage shouldNotBe null
            controller.isLoading shouldBe false
        }
    }
})

private class FakeAuthRepository(
    private val result: AuthResult,
    override val isConfigured: Boolean = true
) : AuthRepository {

    var lastEmail: String? = null
    var lastPassword: String? = null
    var lastDisplayName: String? = null
    var calls = 0

    override fun currentUser(): UserProfile? = null

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        calls++
        lastEmail = email
        lastPassword = password
        return result
    }

    override suspend fun registerWithEmail(
        displayName: String,
        email: String,
        password: String
    ): AuthResult {
        calls++
        lastDisplayName = displayName
        lastEmail = email
        lastPassword = password
        return result
    }

    override suspend fun signInWithGoogle(idToken: String): AuthResult = result

    override suspend fun sendPasswordReset(email: String): AuthResult {
        lastEmail = email
        return result
    }

    override fun signOut() = Unit
}

private class GatedAuthRepository : AuthRepository {
    val gate = CompletableDeferred<AuthResult>()
    var calls = 0

    override val isConfigured = true
    override fun currentUser(): UserProfile? = null

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        calls++
        return gate.await()
    }

    override suspend fun registerWithEmail(
        displayName: String,
        email: String,
        password: String
    ): AuthResult = gate.await()

    override suspend fun signInWithGoogle(idToken: String): AuthResult = gate.await()

    override suspend fun sendPasswordReset(email: String): AuthResult = gate.await()

    override fun signOut() = Unit
}

private class ThrowingAuthRepository : AuthRepository {
    override val isConfigured = true
    override fun currentUser(): UserProfile? = null

    override suspend fun signInWithEmail(email: String, password: String): AuthResult =
        error("Beklenmeyen ağ hatası")

    override suspend fun registerWithEmail(
        displayName: String,
        email: String,
        password: String
    ): AuthResult = error("Beklenmeyen ağ hatası")

    override suspend fun signInWithGoogle(idToken: String): AuthResult =
        error("Beklenmeyen ağ hatası")

    override suspend fun sendPasswordReset(email: String): AuthResult =
        error("Beklenmeyen ağ hatası")

    override fun signOut() = Unit
}
