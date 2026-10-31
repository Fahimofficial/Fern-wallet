package com.ivy.design.system

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring as composeSpring
import androidx.compose.animation.core.tween as composeTween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * The app's one motion system. Every animation takes its timing from here, so motion stays
 * consistent and honours the system "Remove animations" setting everywhere.
 *
 * | Use                                   | Spec                                   |
 * |---------------------------------------|----------------------------------------|
 * | Press feedback, toggles, small fades  | [tween] with [Duration.Fast]           |
 * | Expand/collapse, content changes      | [tween] with [Duration.Standard]       |
 * | Sheets and modals entering/leaving    | [tween] with [Duration.Emphasized]     |
 * | Things that follow a gesture or value | [spring] (slight bounce, interruptible)|
 *
 * With [reduceMotion], tweens take no time and springs settle instantly without overshoot.
 */
object FernMotion {
    /** Set from the activity (animator duration scale 0). Snapshot state, so UI reacts to it. */
    var reduceMotion: Boolean by mutableStateOf(false)

    object Duration {
        const val Fast = 100
        const val Standard = 250
        const val Emphasized = 350
    }

    val StandardEasing: Easing = FastOutSlowInEasing

    /** For things entering the screen: starts fast, settles gently. */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

    /** Stiffness presets for [spring]. */
    object Stiffness {
        const val Fast = 2_000f
        const val Default = 500f
        const val Medium = Spring.StiffnessLow
        const val Slow = Spring.StiffnessVeryLow
        const val VerySlow = 20f
    }

    // < 1% overshoot: lively and interruptible without visible wobble on large surfaces.
    private const val DampingRatio = 0.85f
    private const val ReducedMotionStiffness = 10_000f

    fun <T> tween(durationMillis: Int = Duration.Standard, easing: Easing = StandardEasing): TweenSpec<T> =
        composeTween(durationMillis = if (reduceMotion) 0 else durationMillis, easing = easing)

    fun <T> spring(stiffness: Float = Stiffness.Default, bouncy: Boolean = true): SpringSpec<T> =
        if (reduceMotion) {
            composeSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = ReducedMotionStiffness)
        } else {
            composeSpring(
                dampingRatio = if (bouncy) DampingRatio else Spring.DampingRatioNoBouncy,
                stiffness = stiffness
            )
        }
}
