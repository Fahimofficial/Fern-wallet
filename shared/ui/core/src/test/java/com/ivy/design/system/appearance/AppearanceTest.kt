package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.Test

class AppearanceTest {

    private val custom = AppearanceSettings(
        preset = AppearancePreset.Ivy,
        lightOverrides = mapOf(ColorToken.Income to Color(0xFF0A7A50)),
        darkOverrides = mapOf(ColorToken.Expense to Color(0xFFFF8899), ColorToken.Card to Color(0xFF223344)),
    )

    @Test
    fun `encode then decode keeps preset and both sets of custom colours`() {
        decodeAppearance(custom.encode()) shouldBe custom
    }

    @Test
    fun `missing or corrupt stored value falls back to the defaults`() {
        decodeAppearance(null) shouldBe AppearanceSettings.Default
        decodeAppearance("") shouldBe AppearanceSettings.Default
        decodeAppearance("not a setting") shouldBe AppearanceSettings.Default
        decodeAppearance("v9;preset=Ivy") shouldBe AppearanceSettings.Default
    }

    @Test
    fun `unknown tokens, bad hex and unknown presets are skipped, the rest is kept`() {
        val decoded = decodeAppearance("v1;preset=Gone;D.Nope=FF000000;D.Income=XYZ;D.Card=FF223344;Q.Card=1")
        decoded.preset shouldBe AppearancePreset.Fern
        decoded.darkOverrides shouldContainExactly mapOf(ColorToken.Card to Color(0xFF223344))
        decoded.lightOverrides.shouldBeEmpty()
    }

    @Test
    fun `a custom colour changes only that colour, only in its theme`() {
        val settings = AppearanceSettings.Default.withColor(dark = true, ColorToken.Income, Color.Magenta)
        val dark = settings.resolve(dark = true, trueBlack = false)
        val defaultDark = AppearanceSettings.Default.resolve(dark = true, trueBlack = false)

        dark[ColorToken.Income] shouldBe Color.Magenta
        ColorToken.entries.filter { it != ColorToken.Income }.forEach { dark[it] shouldBe defaultDark[it] }
        settings.resolve(dark = false, trueBlack = false) shouldBe
            AppearanceSettings.Default.resolve(dark = false, trueBlack = false)
    }

    @Test
    fun `AMOLED makes the background black unless the user picked one`() {
        AppearanceSettings.Default.resolve(dark = true, trueBlack = true)[ColorToken.Background] shouldBe Color.Black
        AppearanceSettings.Default.resolve(dark = true, trueBlack = true)[ColorToken.NavBar] shouldBe Color.Black

        val picked = AppearanceSettings.Default.withColor(dark = true, ColorToken.Background, Color(0xFF101820))
        picked.resolve(dark = true, trueBlack = true)[ColorToken.Background] shouldBe Color(0xFF101820)
    }

    @Test
    fun `reset group clears only that group, null colour clears one token, preset clears all`() {
        val reset = custom.resetGroup(dark = true, ColorGroup.Money)
        reset.darkOverrides shouldContainExactly mapOf(ColorToken.Card to Color(0xFF223344))
        reset.lightOverrides shouldContainExactly custom.lightOverrides

        custom.withColor(dark = true, ColorToken.Card, null).darkOverrides.keys shouldBe setOf(ColorToken.Expense)

        val preset = custom.withPreset(AppearancePreset.Midnight)
        preset.preset shouldBe AppearancePreset.Midnight
        preset.lightOverrides.shouldBeEmpty()
        preset.darkOverrides.shouldBeEmpty()
    }

    @Test
    fun `Fern preset keeps the colours the app shipped with`() {
        val dark = AppearanceSettings.Default.resolve(dark = true, trueBlack = false)
        dark[ColorToken.Background] shouldBe Color(0xFF111114)
        dark[ColorToken.Card] shouldBe Color(0xFF2B2C2D)
        dark[ColorToken.Income] shouldBe Color(0xFF14CC9E)
        dark[ColorToken.Accent] shouldBe Color(0xFF13876A)
        dark[ColorToken.Expense] shouldBe dark[ColorToken.TextPrimary]

        val light = AppearanceSettings.Default.resolve(dark = false, trueBlack = false)
        light[ColorToken.Background] shouldBe Color(0xFFFAFAFA)
        light[ColorToken.Card] shouldBe Color(0xFFEFEEF0)
    }

    @Test
    fun `every named preset palette maps every color token in both themes`() {
        AppearancePreset.entries.forEach { preset ->
            listOf(false, true).forEach { dark ->
                preset.colors(dark).keys shouldBe ColorToken.entries.toSet()
            }
        }
    }
}
