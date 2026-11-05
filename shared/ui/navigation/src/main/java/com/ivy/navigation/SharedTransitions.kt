package com.ivy.navigation

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.ivy.design.system.motion.IvyMotion
import java.util.UUID

@OptIn(ExperimentalSharedTransitionApi::class)
@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalScreenAnimationScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Keys that connect an element on one screen to its counterpart on the next. */
object SharedKeys {
    fun transaction(id: UUID): Any = "transaction-$id"
    fun account(id: UUID): Any = "account-$id"
}

/**
 * Container transform between screens: if the screen being opened has an element with the same
 * [key], this element grows into it, and shrinks back when the user returns. Content of both ends
 * crossfades inside the moving bounds, while the rest of each screen uses the normal screen
 * transition. Does nothing when there is no match or outside [NavigationRoot] (previews, tests).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedContainer(key: Any?, shape: Shape): Modifier {
    val sharedScope = LocalSharedTransitionScope.current
    val animationScope = LocalScreenAnimationScope.current
    if (key == null || sharedScope == null || animationScope == null) return this
    return with(sharedScope) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animationScope,
            boundsTransform = ContainerBoundsTransform,
            clipInOverlayDuringTransition = OverlayClip(shape),
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val ContainerBoundsTransform = BoundsTransform { _, _ ->
    // Same length as the screen transition, so the container and the screens land together.
    tween(durationMillis = IvyMotion.Duration.Long, easing = IvyMotion.Easings.Standard)
}
