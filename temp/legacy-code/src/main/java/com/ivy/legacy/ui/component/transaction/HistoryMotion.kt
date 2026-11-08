package com.ivy.legacy.ui.component.transaction

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.MotionPreferences
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Entrance and month-change motion for the history rows of one list (Home).
 *
 * All values are read in graphics layers: animating them redraws rows, it never recomposes them.
 */
@Stable
class HistoryMotion(
    /** Whether the first-load entrance may still play; flip to false once it has. */
    var entrancePending: Boolean,
) {
    /** -1..1: the rows' horizontal offset while a new month slides in (negative = from the left). */
    val slide = Animatable(0f)

    internal fun shouldEnter(index: Int): Boolean =
        entrancePending && index < IvyMotion.Stagger.MaxItems && !MotionPreferences.reduceMotion

    /** Slides the rows in from [fromLeft]'s side. Skipped with "Remove animations". */
    suspend fun slideIn(fromLeft: Boolean) {
        if (MotionPreferences.reduceMotion) return
        slide.snapTo(if (fromLeft) -1f else 1f)
        slide.animateTo(0f, tween(IvyMotion.Duration.Medium, easing = IvyMotion.Easings.EmphasizedDecelerate))
    }

    companion object {
        /** How long the staggered entrance takes from first to last row. */
        const val EntranceMillis = IvyMotion.Stagger.StepMillis * IvyMotion.Stagger.MaxItems + IvyMotion.Duration.Medium
    }
}

private val SlideDistance = 48.dp

/** Fade + 20dp rise, [IvyMotion.Stagger.StepMillis] after the previous row; then follows [HistoryMotion.slide]. */
@Composable
internal fun Modifier.historyMotion(motion: HistoryMotion?, index: Int): Modifier {
    if (motion == null) return this
    val enter = remember { Animatable(if (motion.shouldEnter(index)) 0f else 1f) }
    LaunchedEffect(enter) {
        if (enter.value < 1f) {
            delay(index * IvyMotion.Stagger.StepMillis.toLong())
            enter.animateTo(1f, tween(IvyMotion.Duration.Long, easing = IvyMotion.Easings.EmphasizedDecelerate))
        }
    }
    return graphicsLayer {
        val entered = enter.value
        val slide = motion.slide.value
        alpha = entered * (1f - abs(slide))
        translationY = (1f - entered) * IvyMotion.Stagger.OffsetDp.dp.toPx()
        translationX = slide * SlideDistance.toPx()
    }
}
