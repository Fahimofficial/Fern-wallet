package com.ivy.navigation

import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

/** The opened transaction's card grows into this screen; a new transaction has no card. */
val EditTransactionScreen.sharedKey: Any?
    get() = initialTransactionId?.let(SharedKeys::transaction)

/** An account's card grows into its screen's header; category and filtered lists have no card. */
val TransactionsScreen.sharedKey: Any?
    get() = accountId?.let(SharedKeys::account)

/** The key this screen's container would grow out of, or null when it has none. */
internal fun Screen.containerKey(): Any? = when (this) {
    is EditTransactionScreen -> sharedKey
    is TransactionsScreen -> sharedKey
    else -> null
}

/**
 * Shape of each end of a container transform, so the clip can turn one into the other (a 16dp
 * card into a square screen) instead of keeping the card's corners and popping at the end.
 */
internal class ContainerShapes {
    private val shapes = HashMap<Any, MutableMap<Any, Shape>>()

    fun register(key: Any, end: Any, shape: Shape) {
        shapes.getOrPut(key) { HashMap() }[end] = shape
    }

    fun unregister(key: Any, end: Any) {
        shapes[key]?.let { ends ->
            ends.remove(end)
            if (ends.isEmpty()) shapes.remove(key)
        }
    }

    /** The other end's shape, if it is on screen. */
    fun other(key: Any, end: Any): Shape? = shapes[key]?.entries?.firstOrNull { it.key !== end }?.value
}

@SuppressLint("ComposeCompositionLocalUsage")
internal val LocalContainerShapes = staticCompositionLocalOf<ContainerShapes?> { null }

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
    val shapes = LocalContainerShapes.current
    if (key == null || sharedScope == null || animationScope == null) return this
    val end = remember { Any() }
    DisposableEffect(shapes, key, shape) {
        shapes?.register(key, end, shape)
        onDispose { shapes?.unregister(key, end) }
    }
    // 1 while this end is the one on screen; seekable, so predictive back scrubs the corners too.
    val shown = animationScope.transition.animateFloat(
        transitionSpec = { tween(IvyMotion.Duration.Long, easing = IvyMotion.Easings.Standard) },
        label = "container_corners",
    ) { if (it == EnterExitState.Visible) 1f else 0f }
    val clip = remember(shape, shapes, key) {
        MorphingCornerShape(own = shape, other = { shapes?.other(key, end) ?: shape }, ownFraction = { shown.value })
    }
    return with(sharedScope) {
        this@sharedContainer.sharedBounds(
            sharedContentState = rememberSharedContentState(key),
            animatedVisibilityScope = animationScope,
            boundsTransform = ContainerBoundsTransform,
            // Scales each end's content into the moving bounds instead of re-measuring a whole
            // screen per frame (no text reflow, no relayout while the container grows).
            resizeMode = SharedTransitionScope.ResizeMode.ScaleToBounds(ContentScale.FillWidth, Alignment.TopCenter),
            clipInOverlayDuringTransition = OverlayClip(clip),
        )
    }
}

/**
 * Rounded corners between [other] (fraction 0) and [own] (fraction 1), read at draw time. Shapes
 * that aren't corner-based count as square.
 */
private class MorphingCornerShape(
    private val own: Shape,
    private val other: () -> Shape,
    private val ownFraction: () -> Float,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val t = ownFraction().coerceIn(0f, 1f)
        val from = other()
        fun radius(pick: (CornerBasedShape) -> CornerSize): Float {
            fun of(shape: Shape) = (shape as? CornerBasedShape)?.let { pick(it).toPx(size, density) } ?: 0f
            return of(from) + (of(own) - of(from)) * t
        }
        return Outline.Rounded(
            RoundRect(
                rect = size.toRect(),
                topLeft = CornerRadius(radius { it.topStart }),
                topRight = CornerRadius(radius { it.topEnd }),
                bottomRight = CornerRadius(radius { it.bottomEnd }),
                bottomLeft = CornerRadius(radius { it.bottomStart }),
            )
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val ContainerBoundsTransform = BoundsTransform { _, _ ->
    // Same length as the screen transition, so the container and the screens land together.
    tween(durationMillis = IvyMotion.Duration.Long, easing = IvyMotion.Easings.Standard)
}
