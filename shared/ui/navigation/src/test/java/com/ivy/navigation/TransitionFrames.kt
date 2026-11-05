package com.ivy.navigation

import java.awt.image.BufferedImage

// Reads the frames of SharedContainerTransitionTest: where the (red) destination is drawn.

/** The production key of each destination that has a shared container. */
internal fun Screen.destinationKey(): Any? = when (this) {
    is EditTransactionScreen -> sharedKey
    is TransactionsScreen -> sharedKey
    else -> null
}

internal data class Bounds(val left: Int, val width: Int, val screenWidth: Int)

internal fun List<BufferedImage>.firstDestinationFrame(): Bounds =
    asSequence().map { it.destinationBounds() }.firstOrNull { it.width > 0 }
        ?: error("the destination never appeared in $size frames")

internal fun Bounds.assertFullScreen(): Unit = check(width > screenWidth * FullWidthMin) { "last frame $this" }

internal fun firstFrameMessage(bounds: Bounds): String = "first frame $bounds"

/** Red, possibly fading in over black: the destination; anything else is not. */
internal fun BufferedImage.destinationBounds(): Bounds {
    var left = width
    var right = -1
    for (y in 0 until height step ScanStep) {
        for (x in 0 until width) {
            if (isDestination(getRGB(x, y))) {
                left = minOf(left, x)
                right = maxOf(right, x)
            }
        }
    }
    return Bounds(left = left, width = if (right < 0) 0 else right - left + 1, screenWidth = width)
}

private fun isDestination(rgb: Int): Boolean {
    val r = rgb shr RedShift and ByteMask
    val g = rgb shr GreenShift and ByteMask
    val b = rgb and ByteMask
    return r >= RedMin && g < OtherMax && b < OtherMax
}

internal const val FullWidthMin = 0.9f
private const val ScanStep = 8
private const val RedMin = 48
private const val OtherMax = 32
private const val RedShift = 16
private const val GreenShift = 8
private const val ByteMask = 0xFF
