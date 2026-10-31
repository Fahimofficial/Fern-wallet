package com.ivy.design.utils

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.semantics.Role

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun <T> densityScope(densityScope: @Composable Density.() -> T): T {
    return with(LocalDensity.current) { densityScope() }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Modifier.thenIf(
    condition: Boolean,
    otherModifier: Modifier.() -> Modifier
): Modifier {
    // Cannot use Modifier#then() because it stacks the previous modifiers multiple times
    return if (condition) {
        this.otherModifier()
    } else {
        this
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Modifier.thenWhen(
    logic: Modifier.() -> Modifier?
): Modifier {
    return this.logic() ?: this
}

@Composable
fun rememberInteractionSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * A click without ripple. Pass [role] whenever the element is a real control, so screen readers
 * announce what it is ("Button", "Checkbox"); leave it null only for click-swallowing backdrops.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
fun Modifier.clickableNoIndication(
    interactionSource: MutableInteractionSource,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    return this.clickable(
        interactionSource = interactionSource,
        onClick = onClick,
        role = role,
        indication = null
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@SuppressLint("ComposableNaming")
@Composable
fun onEvent(
    eventKey: Any = Unit,
    cleanUp: () -> Unit = {},
    logic: () -> Unit
) {
    val latestLogic by rememberUpdatedState(logic)
    val latestCleanup by rememberUpdatedState(cleanUp)
    DisposableEffect(eventKey) {
        latestLogic()
        onDispose { latestCleanup() }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun Int.toDensityDp() = densityScope { toDp() }
