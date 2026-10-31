package com.ivy.design.system.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize

/**
 * Fern Wallet's motion system: one place for durations, easings and the standard transitions.
 *
 * Keep routine financial interactions calm: short, decelerating, no bounce or overshoot.
 * All Compose animations already follow the system "animator duration scale", so with
 * "Remove animations" enabled every transition here completes immediately.
 */
object IvyMotion {
    /** Durations in milliseconds. */
    object Duration {
        /** Small state feedback: presses, selections, toggles. */
        const val Short = 150

        /** Content inside a screen: expand/collapse, tab switch, list updates. */
        const val Medium = 250

        /** Whole-screen navigation. */
        const val Long = 300
    }

    /** Material 3 easing curves. */
    object Easings {
        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate: Easing =
            CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate: Easing =
            CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    fun <T> standard(durationMillis: Int = Duration.Medium, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = Easings.Standard)

    private fun <T> enterSpec(
        duration: Int,
        delay: Int = 0,
        easing: Easing = Easings.EmphasizedDecelerate,
    ): FiniteAnimationSpec<T> =
        tween<T>(durationMillis = duration, delayMillis = delay, easing = easing)

    private fun <T> exitSpec(duration: Int, easing: Easing = Easings.EmphasizedAccelerate): FiniteAnimationSpec<T> =
        tween<T>(durationMillis = duration, easing = easing)

    /**
     * Shared-axis (X) navigation between screens. Forward moves content left, back moves it right.
     * The outgoing screen fades out quickly and the incoming one fades in a moment later, so the
     * two screens never fight for attention and no in-between frame is empty.
     */
    fun screenEnter(forward: Boolean): EnterTransition =
        slideInHorizontally(enterSpec<IntOffset>(Duration.Long)) { width ->
            val offset = (width * SlideFraction).toInt()
            if (forward) offset else -offset
        } + fadeIn(enterSpec(Duration.Long - FadeOutDuration, delay = FadeOutDuration, easing = Easings.Standard))

    fun screenExit(forward: Boolean): ExitTransition =
        slideOutHorizontally(exitSpec<IntOffset>(Duration.Long, easing = Easings.Standard)) { width ->
            val offset = (width * SlideFraction).toInt()
            if (forward) -offset else offset
        } + fadeOut(exitSpec(FadeOutDuration))

    fun screenTransform(forward: Boolean): ContentTransform = ContentTransform(
        targetContentEnter = screenEnter(forward),
        initialContentExit = screenExit(forward),
        // Screens are full size: never animate the container's size.
        sizeTransform = NoSizeAnimation,
    )

    /** Fade-through for switching between sibling destinations (bottom-bar tabs). */
    fun fadeThrough(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(
            enterSpec(Duration.Medium - FadeOutDuration, delay = FadeOutDuration, easing = Easings.Standard)
        ),
        initialContentExit = fadeOut(exitSpec(FadeOutDuration)),
        sizeTransform = NoSizeAnimation,
    )

    /** Expanding/collapsing a section inside a screen. */
    fun expandSection(): EnterTransition =
        expandVertically(standard<IntSize>(Duration.Medium)) + fadeIn(standard(Duration.Medium))

    fun collapseSection(): ExitTransition =
        shrinkVertically(standard<IntSize>(Duration.Medium)) + fadeOut(standard(Duration.Short))

    private val NoSizeAnimation = SizeTransform(clip = false) { _, _ -> tween(0) }
    private const val SlideFraction = 0.08f
    private const val FadeOutDuration = 90
}
