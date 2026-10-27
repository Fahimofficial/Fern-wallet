package com.ivy.legacy.utils

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

/**
 * Motion tokens for the legacy UI.
 *
 * - Damping 0.85 (was 0.75): keeps the springy, interruptible feel but cuts overshoot from
 *   ~3% to <1%, so full-screen surfaces (FAB backdrop, More menu) no longer visibly wobble.
 * - Reduced motion: when the user turns animations off in Android's accessibility/developer
 *   settings (animator duration scale = 0), springs become critically damped and near-instant.
 */
object MotionPreferences {
    @Volatile
    var reduceMotion: Boolean = false
}

private const val DampingRatio = 0.85f
private const val ReducedMotionStiffness = 10_000f
private const val VerySlowStiffness = 20f

private fun <T> motionSpring(stiffness: Float): SpringSpec<T> = if (MotionPreferences.reduceMotion) {
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = ReducedMotionStiffness)
} else {
    spring(dampingRatio = DampingRatio, stiffness = stiffness)
}

fun <T> springBounce(
    stiffness: Float = 500f,
): SpringSpec<T> = motionSpring(stiffness)

fun <T> springBounceFast(): SpringSpec<T> = springBounce(
    stiffness = 2000f
)

fun <T> springBounceMedium(): SpringSpec<T> = motionSpring(Spring.StiffnessLow)

fun <T> springBounceSlow(): SpringSpec<T> = motionSpring(Spring.StiffnessVeryLow)

fun <T> springBounceVerySlow(): SpringSpec<T> = motionSpring(VerySlowStiffness)
