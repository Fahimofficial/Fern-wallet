package com.ivy.ui.testing

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllSemanticsNodes
import androidx.compose.ui.semantics.getOrNull
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import app.cash.paparazzi.Snapshot
import app.cash.paparazzi.SnapshotHandler
import org.junit.Rule
import java.awt.image.BufferedImage
import kotlin.math.abs

/**
 * Renders composables with Paparazzi for tests that assert on what was drawn, not on a golden
 * image: nothing is written or compared, so these tests don't need recording.
 */
open class PaparazziRenderTest {
    private val frames = mutableListOf<BufferedImage>()

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6_PRO,
        snapshotHandler = object : SnapshotHandler {
            override fun newFrameHandler(snapshot: Snapshot, frameCount: Int, fps: Int) =
                object : SnapshotHandler.FrameHandler {
                    // Paparazzi may reuse the buffer between frames.
                    override fun handle(image: BufferedImage) {
                        frames += image.copy()
                    }

                    override fun close() = Unit
                }

            override fun close() = Unit
        },
    )

    /** The single frame [content] renders to. */
    protected fun render(content: @Composable () -> Unit): BufferedImage {
        frames.clear()
        paparazzi.snapshot { content() }
        return frames.last()
    }

    /** Frames over [durationMillis], with animations and effects running: for transitions. */
    protected fun renderFrames(
        durationMillis: Long,
        fps: Int,
        content: @Composable () -> Unit,
    ): List<BufferedImage> {
        frames.clear()
        paparazzi.gif(start = 0, end = durationMillis, fps = fps) { content() }
        return frames.toList()
    }
}

private fun BufferedImage.copy(): BufferedImage =
    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also { it.graphics.drawImage(this, 0, 0, null) }

/**
 * How many pixels are [color], each channel within [tolerance] (0..255): a little tolerance lets
 * the subtle gradients of buttons count, without matching unrelated colours.
 */
fun BufferedImage.countPixels(color: Color, tolerance: Int = 0): Int {
    val target = color.toArgb()
    var count = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (channelDistance(getRGB(x, y), target) <= tolerance) count++
        }
    }
    return count
}

private fun channelDistance(a: Int, b: Int): Int = maxOf(
    abs((a shr RedShift and ByteMask) - (b shr RedShift and ByteMask)),
    abs((a shr GreenShift and ByteMask) - (b shr GreenShift and ByteMask)),
    abs((a and ByteMask) - (b and ByteMask)),
)

/**
 * Performs a click on the composable tagged [tag] the way a user tap would trigger it (its
 * semantics click action), for Paparazzi tests that have no input injection. Call it once the
 * tree exists, e.g. from a layout callback. Returns false when there's no clickable [tag].
 */
fun View.performClickOnTag(tag: String): Boolean {
    val click = (this as? RootForTest)?.semanticsOwner
        ?.getAllSemanticsNodes(mergingEnabled = false)
        ?.firstOrNull { it.config.getOrNull(SemanticsProperties.TestTag) == tag }
        ?.config?.getOrNull(SemanticsActions.OnClick)?.action
    return click?.invoke() ?: false
}

private const val RedShift = 16
private const val GreenShift = 8
private const val ByteMask = 0xFF
