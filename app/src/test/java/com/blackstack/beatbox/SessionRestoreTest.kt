package com.blackstack.beatbox

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Yerel profil tercihlerinin Firebase oturumuyla birleştirilmesini doğrular.
 * Aynı hesapta bio/avatar korunmalı, farklı hesapta taşınmamalı.
 */
class SessionRestoreTest : StringSpec({

    val local = UserProfile(
        id = "local-id",
        displayName = "Yerel Ad",
        email = "deniz@example.com",
        bio = "Gece kuşu",
        avatarId = 3,
        isLoggedIn = true,
        isPremium = false
    )

    val remote = UserProfile(
        id = "firebase-uid",
        displayName = "Deniz",
        email = "deniz@example.com",
        bio = "Uzak bio",
        avatarId = 1,
        isLoggedIn = true,
        authProvider = AuthProvider.GOOGLE,
        isPremium = true
    )

    "aynı e-posta ile yerel bio avatar ve premium korunur" {
        val merged = mergeSessionProfile(local, remote)

        merged.id shouldBe "firebase-uid"
        merged.displayName shouldBe "Deniz"
        merged.authProvider shouldBe AuthProvider.GOOGLE
        merged.bio shouldBe "Gece kuşu"
        merged.avatarId shouldBe 3
        merged.isPremium shouldBe false
    }

    "e-posta büyük küçük harf farkı eşleşmeyi bozmaz" {
        val merged = mergeSessionProfile(
            local.copy(email = "DENIZ@example.com"),
            remote
        )

        merged.bio shouldBe "Gece kuşu"
        merged.avatarId shouldBe 3
    }

    "farklı hesapta yerel tercihler taşınmaz" {
        val merged = mergeSessionProfile(
            local.copy(email = "baska@example.com"),
            remote
        )

        merged.bio shouldBe "Uzak bio"
        merged.avatarId shouldBe 1
        merged.isPremium shouldBe true
    }

    "uzak e-posta boşsa eşleşme sayılmaz" {
        val merged = mergeSessionProfile(local, remote.copy(email = ""))

        merged.bio shouldBe "Uzak bio"
        merged.avatarId shouldBe 1
    }
})
