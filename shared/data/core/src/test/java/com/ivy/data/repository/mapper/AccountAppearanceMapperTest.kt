package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.model.AccountAppearance
import com.ivy.data.model.AccountBackgroundStyle
import io.kotest.matchers.shouldBe
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Test

class AccountAppearanceMapperTest {
    private val plain = AccountEntity(name = "Wallet", color = 1)

    @Test
    fun `an existing account (all columns null) has the original look`() {
        plain.appearance() shouldBe AccountAppearance.Default
    }

    @Test
    fun `the default appearance writes nulls, as an untouched account has`() {
        plain.copy(logoKey = "visa", backgroundStyle = "Image").withAppearance(AccountAppearance.Default) shouldBe plain
    }

    @Test
    fun `a custom appearance survives the round trip`() {
        val custom = AccountAppearance(
            logoKey = "hdfcbank",
            customLogoPath = "/data/account_media/logo.png",
            backgroundStyle = AccountBackgroundStyle.Gradient,
            backgroundColors = listOf(0xFFE91E63.toInt(), 0xFF3F51B5.toInt()),
            gradientAngle = 90f,
            backgroundImagePath = null,
            backgroundDim = 0.7f,
        )
        plain.withAppearance(custom).appearance() shouldBe custom
    }

    @Test
    fun `unknown or broken values fall back instead of failing`() {
        val broken = plain.copy(backgroundStyle = "Holographic", backgroundColors = "zz, FF00FF00", backgroundDim = 3f)
        broken.appearance().backgroundStyle shouldBe AccountBackgroundStyle.Solid
        broken.appearance().backgroundColors shouldBe listOf(0xFF00FF00.toInt())
        broken.appearance().backgroundDim shouldBe 1f
    }

    @Test
    fun `backups stay compatible both ways`() {
        val json = Json { ignoreUnknownKeys = true }
        // A backup from before this change has no appearance fields.
        val old = """{"name":"Wallet","color":1,"id":"00000000-0000-0001-0000-000000000001"}"""
        json.decodeFromString<AccountEntity>(old).appearance() shouldBe AccountAppearance.Default
        // An untouched account writes no new keys, so older app versions read it as before.
        json.encodeToString(plain).contains("background") shouldBe false
        val styled = plain.withAppearance(AccountAppearance(logoKey = "visa"))
        json.decodeFromString<AccountEntity>(json.encodeToString(styled)).appearance().logoKey shouldBe "visa"
    }
}
