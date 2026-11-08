package com.ivy.legacy.ui.component.transaction

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.MotionPreferences
import kotlinx.coroutines.delay
import java.util.UUID

/**
 * After the edit screen shrinks back into its row, that row glows once, so the eye finds what
 * just changed. The list is rebuilt on return, so the row's first composition consumes the mark.
 */
internal object ReturnPulse {
    private var openedId: UUID? = null

    fun opened(id: UUID) {
        openedId = id
    }

    fun consume(id: UUID): Boolean = (openedId == id).also { if (it) openedId = null }
}

@Composable
internal fun Modifier.returnPulse(id: UUID): Modifier {
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(id) {
        if (!ReturnPulse.consume(id) || MotionPreferences.reduceMotion) return@LaunchedEffect
        delay(IvyMotion.Duration.Long.toLong()) // let the container transform land first
        pulse.animateTo(1f, IvyMotion.standard(IvyMotion.Duration.Short))
        pulse.animateTo(0f, IvyMotion.standard(IvyMotion.Duration.Long))
    }
    val color = FernTheme.palette[ColorToken.Accent]
    return drawWithContent {
        drawContent()
        val value = pulse.value
        if (value > 0f) drawRect(color, alpha = value * PulseAlpha)
    }
}

private const val PulseAlpha = 0.18f
