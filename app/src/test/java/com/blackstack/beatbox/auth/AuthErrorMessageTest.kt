package com.blackstack.beatbox.auth

import com.blackstack.beatbox.AppLanguage
import com.blackstack.beatbox.Strings
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

/**
 * Firebase hata kodlarının yerelleştirilmiş kullanıcı mesajlarına
 * çevrildiğini doğrular. Bilinmeyen kodlarda teknik metin sızmaz.
 */
class AuthErrorMessageTest : StringSpec({

    val tr = Strings(AppLanguage.TR)
    val en = Strings(AppLanguage.EN)

    "geçersiz e-posta kodu anlaşılır mesaja döner" {
        authErrorMessage("ERROR_INVALID_EMAIL", tr) shouldContain "Geçerli bir e-posta"
    }

    "hatalı şifre ve kimlik bilgisi aynı mesajı paylaşır" {
        authErrorMessage("ERROR_WRONG_PASSWORD", tr) shouldBe "E-posta veya şifre hatalı."
        authErrorMessage("ERROR_INVALID_CREDENTIAL", tr) shouldBe "E-posta veya şifre hatalı."
    }

    "kayıtlı olmayan hesap ayrı mesaj verir" {
        authErrorMessage("ERROR_USER_NOT_FOUND", tr) shouldContain "kayıtlı bir hesap bulunamadı"
    }

    "zaten kullanılan e-posta bildirilir" {
        authErrorMessage("ERROR_EMAIL_ALREADY_IN_USE", tr) shouldContain "zaten kullanılıyor"
    }

    "zayıf şifre en az altı karakter ister" {
        authErrorMessage("ERROR_WEAK_PASSWORD", tr) shouldContain "en az 6 karakter"
    }

    "çok fazla denemede beklemeye yönlendirir" {
        authErrorMessage("ERROR_TOO_MANY_REQUESTS", tr) shouldContain "biraz sonra tekrar deneyin"
    }

    "ağ hatası internet bağlantısını işaret eder" {
        authErrorMessage("ERROR_NETWORK_REQUEST_FAILED", tr) shouldContain "İnternet bağlantınızı"
    }

    "e-posta sağlayıcısı kapalıysa konsol ayarı söylenir" {
        authErrorMessage("ERROR_OPERATION_NOT_ALLOWED", tr) shouldContain "Firebase konsolunda"
    }

    "bilinmeyen kod genel mesaja döner" {
        authErrorMessage("ERROR_BILINMEYEN_KOD", tr) shouldBe
            "Giriş yapılamadı. Lütfen tekrar deneyin."
    }

    "null kod genel mesaja döner" {
        authErrorMessage(null, tr) shouldBe "Giriş yapılamadı. Lütfen tekrar deneyin."
    }

    "mesajlar seçilen dile göre üretilir" {
        authErrorMessage("ERROR_INVALID_EMAIL", en) shouldBe "Enter a valid email address."
        authErrorMessage("ERROR_WRONG_PASSWORD", en) shouldBe "Incorrect email or password."
    }
})
