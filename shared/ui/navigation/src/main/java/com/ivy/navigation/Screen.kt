package com.ivy.navigation

import androidx.compose.runtime.Immutable

/**
 * Marks a screen in the Ivy Wallet's navigation graph.
 * Extend it when creating a new screen.
 *
 * Screens are immutable navigation arguments (data classes/objects with `val`s only); keep it so.
 */
@Immutable
sealed interface Screen {
    /**
     * Marks whether a given screen is a legacy Ivy Wallet one.
     * If it's a legacy screen, it automatically adds a Surface to make it work.
     * Do NOT mark new Material3 screens as legacy: give them an opaque root instead (a Scaffold,
     * or `Modifier.background(UI.colors.pure)`). A transparent screen shows the one behind it
     * through the transition.
     */
    val isLegacy: Boolean
        get() = false
}
