package com.ivy.design.system.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.node.LayoutModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Constraints
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** How much a pressed control shrinks. Small enough to read as "pressed", not as "moving". */
const val PressedScale = 0.96f

/**
 * Shrinks the element while [interactionSource] reports a press and springs it back on release.
 *
 * Pass the same [InteractionSource] to `clickable`/`selectable`. The scale lives in a graphics
 * layer, so pressing only redraws the element: no recomposition, no relayout. A new press or
 * release re-targets the running spring from its current value and velocity, so rapid taps never
 * jump. Put it before `clip`/`background`/shadow so the whole control scales together.
 */
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = PressedScale,
): Modifier = this then PressScaleElement(interactionSource, pressedScale)

private data class PressScaleElement(
    val interactionSource: InteractionSource,
    val pressedScale: Float,
) : ModifierNodeElement<PressScaleNode>() {
    override fun create(): PressScaleNode = PressScaleNode(interactionSource, pressedScale)

    override fun update(node: PressScaleNode) {
        node.pressedScale = pressedScale
        if (node.interactionSource != interactionSource) {
            node.interactionSource = interactionSource
            node.observe()
        }
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "pressScale"
        properties["pressedScale"] = pressedScale
    }
}

private class PressScaleNode(
    var interactionSource: InteractionSource,
    var pressedScale: Float,
) : Modifier.Node(), LayoutModifierNode {
    private val scale = Animatable(1f)
    private var observeJob: Job? = null

    override fun onAttach() {
        observe()
    }

    fun observe() {
        if (!isAttached) return
        observeJob?.cancel()
        observeJob = coroutineScope.launch {
            // A node reused by a lazy list may have been detached mid-press.
            scale.snapTo(1f)
            val presses = mutableListOf<PressInteraction.Press>()
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> presses += interaction
                    is PressInteraction.Release -> presses -= interaction.press
                    is PressInteraction.Cancel -> presses -= interaction.press
                }
                val pressed = presses.isNotEmpty()
                launch {
                    scale.animateTo(
                        targetValue = if (pressed) pressedScale else 1f,
                        animationSpec = if (pressed) IvyMotion.Springs.press() else IvyMotion.Springs.release(),
                    )
                }
            }
        }
    }

    override fun onDetach() {
        observeJob = null
    }

    override fun MeasureScope.measure(measurable: Measurable, constraints: Constraints): MeasureResult {
        val placeable = measurable.measure(constraints)
        return layout(placeable.width, placeable.height) {
            placeable.placeWithLayer(0, 0) {
                val value = scale.value
                scaleX = value
                scaleY = value
            }
        }
    }
}
