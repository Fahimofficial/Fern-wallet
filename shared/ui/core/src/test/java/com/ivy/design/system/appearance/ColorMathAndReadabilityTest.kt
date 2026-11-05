package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.junit.Test

class ColorMathAndReadabilityTest {

    @Test
    fun `every preset passes its own readability rules in light, dark and AMOLED`() {
        val failures = AppearancePreset.entries.flatMap { preset ->
            listOf(false to false, true to false, true to true).map { (dark, amoled) ->
                val palette = AppearanceSettings.Default.withPreset(preset).resolve(dark, amoled)
                "$preset dark=$dark amoled=$amoled" to palette
            }
        }.flatMap { (name, palette) ->
            ColorToken.entries.flatMap { token ->
                palette.contrastChecks(token, palette[token])
                    .filterNot { it.passes }
                    .map { "$name $token vs ${it.against}: ${it.ratio}" }
            }
        }
        failures shouldBe emptyList()
    }

    @Test
    fun `unreadable text colour is rejected`() {
        val dark = AppearanceSettings.Default.resolve(dark = true, trueBlack = false)
        val checks = dark.contrastChecks(ColorToken.TextPrimary, Color(0xFF222226))
        checks.any { !it.passes } shouldBe true
        dark.contrastChecks(ColorToken.TextPrimary, Color.White).all { it.passes } shouldBe true
    }

    @Test
    fun `contrast and readable colours`() {
        contrastRatio(Color.Black, Color.White) shouldBe (21f plusOrMinus 0.01f)
        contrastRatio(Color.Red, Color.Red) shouldBe (1f plusOrMinus 0.001f)
        val white = Color(0xFFFAFAFA)
        (contrastRatio(Color(0xFF14CC9E).readableOn(white, 4.5f), white) >= 4.5f) shouldBe true
        Color.White.contentColor() shouldNotBe Color.White
        Color.Black.contentColor() shouldNotBe Color.Black
    }

    @Test
    fun `hex parsing`() {
        parseHexColor("#13876A") shouldBe Color(0xFF13876A)
        parseHexColor("13876a") shouldBe Color(0xFF13876A)
        parseHexColor("8013876A") shouldBe Color(0x8013876A)
        parseHexColor("12345") shouldBe null
        parseHexColor("GGGGGG") shouldBe null
        Color(0xFF13876A).toHex() shouldBe "13876A"
        Color(0x8013876A).toHex(withAlpha = true) shouldBe "8013876A"
    }
}
