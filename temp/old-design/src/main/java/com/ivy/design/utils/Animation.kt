package com.ivy.design.utils

import androidx.compose.animation.core.SpringSpec
import com.ivy.design.system.FernMotion

/** Same motion as the rest of the app (it used a bouncier spring before); see [FernMotion]. */
@Deprecated("Old design system. Use FernMotion")
fun <T> springBounce(
    stiffness: Float = FernMotion.Stiffness.Default,
): SpringSpec<T> = FernMotion.spring(stiffness)
