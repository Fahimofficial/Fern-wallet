package com.ivy.legacy.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.offset

/*
 * Window-inset helpers for the legacy overlays (sheets, action rows).
 *
 * They read the raw inset during layout, so a moving keyboard relayouts the element on every
 * frame of the system IME animation (API 30+) without recomposing it, and the element tracks the
 * keyboard exactly instead of chasing it with its own tween. Unlike `windowInsetsPadding`, they
 * ignore insets consumed by ancestors: legacy overlays are positioned against the whole screen,
 * even when they sit inside a screen that pads itself for the system bars.
 */

/** Adds bottom padding equal to the bottom of [insets]. */
fun Modifier.bottomInsetPadding(insets: WindowInsets): Modifier = layout { measurable, constraints ->
    val bottom = insets.getBottom(this)
    val placeable = measurable.measure(constraints.offset(vertical = -bottom))
    val height = (placeable.height + bottom).coerceIn(constraints.minHeight, constraints.maxHeight)
    layout(placeable.width, height) {
        placeable.place(0, 0)
    }
}

/** Makes the element exactly as tall as the bottom of [insets] (use on a `Spacer`). */
fun Modifier.bottomInsetHeight(insets: WindowInsets): Modifier = layout { measurable, constraints ->
    val height = insets.getBottom(this).coerceIn(constraints.minHeight, constraints.maxHeight)
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, height) {
        placeable.place(0, 0)
    }
}
