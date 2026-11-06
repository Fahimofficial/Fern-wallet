package com.ivy.design.system.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

/** WCAG AA for normal text. */
const val MinTextContrast = 4.5f

/** The lowest contrast [text] reaches over any of [backgrounds] (a gradient's stops). */
fun minContrast(text: Color, backgrounds: List<Color>): Float =
    backgrounds.minOfOrNull { contrastRatio(text, it) } ?: Float.MAX_VALUE

/** Black or white, whichever keeps the higher worst-case contrast over all [backgrounds]. */
fun contentColorFor(backgrounds: List<Color>): Color {
    return if (minContrast(DarkText, backgrounds) >= minContrast(LightText, backgrounds)) DarkText else LightText
}

/**
 * The least alpha of a black scrim over [backgrounds] that lets white text reach [minContrast]
 * on the brightest of them (0 when it already does). For an image, pass [Color.White]: the scrim
 * then holds for any pixel, with no sampling.
 */
fun scrimAlphaForWhiteText(backgrounds: List<Color>, minContrast: Float = MinTextContrast): Float {
    var step = 0
    while (step <= ScrimSteps) {
        val alpha = step / ScrimSteps.toFloat()
        val darkened = backgrounds.map { lerp(it, Color.Black, alpha) }
        if (minContrast(LightText, darkened) >= minContrast) return alpha
        step++
    }
    return 1f
}

/** What sits on an account card: the text colour and how much black scrim it needs under it. */
data class CardInk(val content: Color, val scrimAlpha: Float)

/**
 * Text colour and scrim for a card painted with [backgrounds] (one colour or a gradient's stops):
 * black or white text without a scrim when either reaches 4.5:1 everywhere, otherwise white text
 * on the smallest scrim that gets there.
 */
fun cardInk(backgrounds: List<Color>): CardInk {
    val best = contentColorFor(backgrounds)
    return if (minContrast(best, backgrounds) >= MinTextContrast) {
        CardInk(best, 0f)
    } else {
        CardInk(LightText, scrimAlphaForWhiteText(backgrounds))
    }
}

// The app's own near-black and near-white text colours (see contentColor).
private val DarkText = Color.White.contentColor()
private val LightText = Color.Black.contentColor()

private const val ScrimSteps = 100
