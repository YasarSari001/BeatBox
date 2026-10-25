package com.blackstack.beatbox.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Firebase hata kodlarının kullanıcıya gösterilebilir Türkçe mesajlara
 * çevrildiğini doğrular. Bilinmeyen kodlarda teknik İngilizce metin sızmaz.
 */
class AuthErrorMessageTest : StringSpec({

    "geçersiz e-posta kodu anlaşılır mesaja döner" {
        authErrorMessage("ERROR_INVALID_EMAIL") shouldContain "Geçerli bir e-posta"
    }

    "hatalı şifre ve kimlik bilgisi aynı mesajı paylaşır" {
        authErrorMessage("ERROR_WRONG_PASSWORD") shouldBe "E-posta veya şifre hatalı."
        authErrorMessage("ERROR_INVALID_CREDENTIAL") shouldBe "E-posta veya şifre hatalı."
    }

    "kayıtlı olmayan hesap ayrı mesaj verir" {
        authErrorMessage("ERROR_USER_NOT_FOUND") shouldContain "kayıtlı bir hesap bulunamadı"
    }

    "zaten kullanılan e-posta bildirilir" {
        authErrorMessage("ERROR_EMAIL_ALREADY_IN_USE") shouldContain "zaten kullanılıyor"
    }

    "zayıf şifre en az altı karakter ister" {
        authErrorMessage("ERROR_WEAK_PASSWORD") shouldContain "en az 6 karakter"
    }

    "çok fazla denemede beklemeye yönlendirir" {
        authErrorMessage("ERROR_TOO_MANY_REQUESTS") shouldContain "biraz sonra tekrar deneyin"
    }

    "ağ hatası internet bağlantısını işaret eder" {
        authErrorMessage("ERROR_NETWORK_REQUEST_FAILED") shouldContain "İnternet bağlantınızı"
    }

    "e-posta sağlayıcısı kapalıysa konsol ayarı söylenir" {
        authErrorMessage("ERROR_OPERATION_NOT_ALLOWED") shouldContain "Firebase konsolunda"
    }

    "bilinmeyen kod genel mesaja döner" {
        authErrorMessage("ERROR_BILINMEYEN_KOD") shouldBe
            "Giriş yapılamadı. Lütfen tekrar deneyin."
    }

    "null kod genel mesaja döner" {
        authErrorMessage(null) shouldBe "Giriş yapılamadı. Lütfen tekrar deneyin."
    }
})
