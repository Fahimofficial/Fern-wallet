package com.ivy.legacy.utils

import androidx.compose.animation.core.SpringSpec
import com.ivy.design.system.FernMotion

/**
 * Legacy names for the motion tokens in [FernMotion]; prefer FernMotion in new code.
 */
object MotionPreferences {
    var reduceMotion: Boolean
        get() = FernMotion.reduceMotion
        set(value) {
            FernMotion.reduceMotion = value
        }
}

fun <T> springBounce(
    stiffness: Float = FernMotion.Stiffness.Default,
): SpringSpec<T> = FernMotion.spring(stiffness)

fun <T> springBounceFast(): SpringSpec<T> = FernMotion.spring(FernMotion.Stiffness.Fast)

fun <T> springBounceMedium(): SpringSpec<T> = FernMotion.spring(FernMotion.Stiffness.Medium)

fun <T> springBounceSlow(): SpringSpec<T> = FernMotion.spring(FernMotion.Stiffness.Slow)

fun <T> springBounceVerySlow(): SpringSpec<T> = FernMotion.spring(FernMotion.Stiffness.VerySlow)
