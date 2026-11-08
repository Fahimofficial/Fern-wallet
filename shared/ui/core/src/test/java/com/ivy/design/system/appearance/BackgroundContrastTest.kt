package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.kotest.matchers.floats.shouldBeGreaterThanOrEqual
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.shouldBe
import org.junit.Test

class BackgroundContrastTest {
    private val pink = Color(0xFFE91E63)
    private val navy = Color(0xFF1A237E)
    private val yellow = Color(0xFFFFEB3B)

    @Test
    fun `solid colours get the readable one of black and white, without a scrim`() {
        cardInk(listOf(yellow)).let { ink ->
            minContrast(ink.content, listOf(yellow)) shouldBeGreaterThanOrEqual MinTextContrast
            ink.scrimAlpha shouldBe 0f
        }
        cardInk(listOf(navy)).content shouldBe Color.Black.contentColor()
    }

    @Test
    fun `a gradient no single colour can read on gets a scrim that reaches 4_5 on every stop`() {
        val stops = listOf(yellow, navy) // black fails on navy, white fails on yellow
        val ink = cardInk(stops)
        ink.scrimAlpha shouldBeLessThan 1f
        val darkened = stops.map { lerp(it, Color.Black, ink.scrimAlpha) }
        minContrast(ink.content, darkened) shouldBeGreaterThanOrEqual MinTextContrast
    }

    @Test
    fun `the image scrim holds even over a pure white pixel`() {
        val alpha = scrimAlphaForWhiteText(listOf(Color.White))
        val text = Color.Black.contentColor()
        minContrast(text, listOf(lerp(Color.White, Color.Black, alpha))) shouldBeGreaterThanOrEqual MinTextContrast
        // ...and it's the least such scrim (one step less fails).
        minContrast(text, listOf(lerp(Color.White, Color.Black, alpha - 0.01f))) shouldBeLessThan MinTextContrast
    }

    @Test
    fun `no scrim when white already reads`() {
        scrimAlphaForWhiteText(listOf(navy, pink.copy(red = 0.5f))) shouldBe 0f
    }
}
