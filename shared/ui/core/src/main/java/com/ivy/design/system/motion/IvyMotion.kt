package com.ivy.design.system.motion

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI

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

        /** Whole-screen navigation: a screen arriving. */
        const val Long = 300

        /** Whole-screen navigation: a screen leaving (exits are quicker than entrances). */
        const val Exit = 200

        /** Nothing in the app animates for longer than this. */
        const val Max = 350
    }

    /** List entrance: each item starts [StepMillis] after the previous one, only the first [MaxItems]. */
    object Stagger {
        const val StepMillis = 35
        const val MaxItems = 8

        /** Distance an entering item travels up while it fades in. */
        const val OffsetDp = 20
    }

    /** Material 3 easing curves. */
    object Easings {
        val Standard: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
        val EmphasizedDecelerate: Easing =
            CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
        val EmphasizedAccelerate: Easing =
            CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
    }

    /**
     * Springs for anything the user can touch or interrupt (sheets, drags, presses).
     *
     * A spring starts from the value currently on screen and keeps its velocity when the target
     * changes, so the motion can be grabbed and reversed mid-flight without a jump. Parameters are
     * given as damping ratio (1 = no overshoot) and response (seconds to reach the target), the
     * pair Apple's fluid-interface guidelines use, then converted to Compose's stiffness.
     */
    object Springs {
        /** Default for sheets and surfaces: critically damped, no overshoot. */
        fun <T> surface(): SpringSpec<T> = spring(dampingRatio = 1f, response = 0.3f)

        /** Content shrinking under the finger: immediate and critically damped. */
        fun <T> press(): SpringSpec<T> = spring(dampingRatio = 1f, response = 0.12f)

        /** Release after a press or a drag: slightly under-damped because the finger gave it momentum. */
        fun <T> release(): SpringSpec<T> = spring(dampingRatio = 0.8f, response = 0.3f)

        /** A small, visible bounce for a selection landing (nav icons, sliding thumbs). */
        fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.55f, response = 0.3f)

        /** @param response seconds to (approximately) reach the target; not a fixed duration. */
        fun <T> spring(dampingRatio: Float, response: Float): SpringSpec<T> {
            val omega = 2f * PI.toFloat() / response
            return androidx.compose.animation.core.spring(
                dampingRatio = dampingRatio,
                stiffness = omega * omega,
            )
        }
    }

    /**
     * Where a flick would come to rest, using the exponential scroll deceleration of
     * Apple's "Designing Fluid Interfaces" sample: choose snap targets from this, not from the
     * release position, so a short fast flick still counts.
     *
     * @param velocity px per second.
     */
    fun projectedDistance(velocity: Float, decelerationRate: Float = ScrollDecelerationRate): Float =
        velocity / MillisPerSecond * decelerationRate / (1f - decelerationRate)

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
     * Navigation between screens. The screen in front moves (forward: slides in 8% from the
     * right and fades in over [Duration.Long]; back: slides out and fades over [Duration.Exit]).
     * The screen behind it never vanishes: it stays put while the host scales and dims it (see
     * `NavigationRoot`), so the two read as layers, and back runs the same path in reverse.
     *
     * @param container the front screen grows out of an element of the screen behind (container
     * transform): the shared bounds drive that motion, so the screen itself only fades.
     */
    fun screenTransform(forward: Boolean, container: Boolean = false): ContentTransform =
        if (forward) {
            ContentTransform(
                targetContentEnter = if (container) {
                    fadeIn(enterSpec(Duration.Short))
                } else {
                    slideInHorizontally(enterSpec<IntOffset>(Duration.Long)) { (it * SlideFraction).toInt() } +
                        fadeIn(enterSpec(Duration.Long))
                },
                // Stays fully drawn (alpha 1 -> 1) for the whole transition; the host recedes it.
                initialContentExit = fadeOut(enterSpec(Duration.Long), targetAlpha = 1f),
                sizeTransform = NoSizeAnimation,
            )
        } else {
            ContentTransform(
                targetContentEnter = EnterTransition.None,
                initialContentExit = if (container) {
                    fadeOut(exitSpec(Duration.Exit))
                } else {
                    slideOutHorizontally(exitSpec<IntOffset>(Duration.Exit)) { (it * SlideFraction).toInt() } +
                        fadeOut(exitSpec(Duration.Exit))
                },
                // The leaving screen stays on top of the one it reveals.
                targetContentZIndex = -1f,
                sizeTransform = NoSizeAnimation,
            )
        }

    /**
     * Shared-axis (X) between sibling steps that replace each other with no layer behind
     * (onboarding): the outgoing step fades out quickly, the incoming one fades in a moment later.
     */
    fun sharedAxisX(forward: Boolean): ContentTransform = ContentTransform(
        targetContentEnter = slideInHorizontally(enterSpec<IntOffset>(Duration.Long)) { width ->
            val offset = (width * SlideFraction).toInt()
            if (forward) offset else -offset
        } + fadeIn(enterSpec(Duration.Long - FadeOutDuration, delay = FadeOutDuration, easing = Easings.Standard)),
        initialContentExit = slideOutHorizontally(
            exitSpec<IntOffset>(Duration.Long, easing = Easings.Standard)
        ) { width ->
            val offset = (width * SlideFraction).toInt()
            if (forward) -offset else offset
        } + fadeOut(exitSpec(FadeOutDuration)),
        sizeTransform = NoSizeAnimation,
    )

    /** How far the screen behind recedes: scale and black-scrim alpha when fully covered. */
    const val RecededScale = 0.94f
    const val RecededDim = 0.32f

    /** Timing of the screen behind as it recedes or comes back; same as the front screen's entrance. */
    fun <T> recedeSpec(): FiniteAnimationSpec<T> = enterSpec(Duration.Long)

    /**
     * The screen behind coming back to full size when the screen in front leaves. Lasts as long as
     * that exit ([Duration.Exit]), so the two land together instead of the back layer still
     * settling after the front one is gone. Decelerates, so it comes to rest without a jolt.
     */
    fun <T> recedeBackSpec(): FiniteAnimationSpec<T> = enterSpec(Duration.Exit)

    /** Fade-through for switching between sibling destinations (bottom-bar tabs). */
    fun fadeThrough(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(fadeThroughIn()),
        initialContentExit = fadeOut(fadeThroughOut()),
        sizeTransform = NoSizeAnimation,
    )

    /** Fade-through timing for the incoming content: starts once the outgoing one is gone. */
    fun <T> fadeThroughIn(): FiniteAnimationSpec<T> =
        enterSpec(Duration.Medium - FadeOutDuration, delay = FadeOutDuration, easing = Easings.Standard)

    /** Fade-through timing for the outgoing content. */
    fun <T> fadeThroughOut(): FiniteAnimationSpec<T> = exitSpec(FadeOutDuration)

    /** Expanding/collapsing a section inside a screen. */
    fun expandSection(): EnterTransition =
        expandVertically(standard<IntSize>(Duration.Medium)) + fadeIn(standard(Duration.Medium))

    fun collapseSection(): ExitTransition =
        shrinkVertically(standard<IntSize>(Duration.Medium)) + fadeOut(standard(Duration.Short))

    private val NoSizeAnimation = SizeTransform(clip = false) { _, _ -> tween(0) }
    private const val SlideFraction = 0.08f
    private const val FadeOutDuration = 90
    private const val ScrollDecelerationRate = 0.998f
    private const val MillisPerSecond = 1000f
}
