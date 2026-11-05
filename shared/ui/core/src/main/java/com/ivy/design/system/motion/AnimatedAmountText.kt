package com.ivy.design.system.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow

/** OpenType "tabular figures": every digit has the same width, so changing numbers don't jitter. */
const val TabularNumbers = "tnum"

fun TextStyle.tabularNumbers(): TextStyle = copy(fontFeatureSettings = TabularNumbers)

/** The value shown at [progress] (0..1) of a count from [from] to [to]; exactly [to] at the end. */
fun countUpValue(from: Double, to: Double, progress: Float): Double =
    if (progress >= 1f) to else from + (to - from) * progress

/**
 * Shows [amount] and counts to each new value from the one on screen.
 *
 * The first composition shows [amount] as is, so rows re-entering a list never re-animate; only a
 * changed value (data loaded, month switched) counts. Only this Text recomposes while counting.
 * TalkBack always reads the final value. With "Remove animations" on, the new value shows at once.
 */
@Composable
fun AnimatedAmountText(
    amount: Double,
    format: (Double) -> String,
    style: TextStyle,
    modifier: Modifier = Modifier,
) {
    var from by remember { mutableDoubleStateOf(amount) }
    var to by remember { mutableDoubleStateOf(amount) }
    val progress = remember { Animatable(1f) }
    LaunchedEffect(amount) {
        if (amount == to) return@LaunchedEffect
        from = countUpValue(from, to, progress.value)
        to = amount
        if (MotionPreferences.reduceMotion) {
            progress.snapTo(1f)
        } else {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(IvyMotion.Duration.Long, easing = IvyMotion.Easings.EmphasizedDecelerate),
            )
        }
    }
    val finalText = format(amount)
    Text(
        modifier = modifier.clearAndSetSemantics { contentDescription = finalText },
        // From `to`, not `amount`: until the effect starts, a new amount must not flash for a frame.
        text = format(countUpValue(from, to, progress.value)),
        style = style.tabularNumbers(),
        maxLines = 1,
        overflow = TextOverflow.Clip,
    )
}
