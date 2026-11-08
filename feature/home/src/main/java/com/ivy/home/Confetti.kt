package com.ivy.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import com.ivy.design.system.motion.MotionPreferences
import kotlin.random.Random

private const val ConfettiMillis = 800
private const val PieceCount = 36
private const val LaunchHeight = 0.5f
private const val Gravity = 1.6f
private val ConfettiTokens = listOf(ColorToken.Income, ColorToken.Accent, ColorToken.Upcoming, ColorToken.Transfer)

private class Piece(val x: Float, val vx: Float, val vy: Float, val spin: Float, val color: Color)

/**
 * A short burst over the savings-goal card, drawn on one Canvas (no recomposition per frame).
 * Plays once, then [onFinished]; with "Remove animations" it reports finished without drawing.
 */
@Composable
internal fun Confetti(modifier: Modifier = Modifier, onFinished: () -> Unit) {
    val palette = FernTheme.palette
    val colors = ConfettiTokens.map { palette[it] }
    val pieces = remember {
        val random = Random(seed = 7)
        List(PieceCount) {
            Piece(
                x = random.nextFloat(),
                vx = random.nextFloat() - 0.5f,
                vy = 0.6f + random.nextFloat() * 0.6f,
                spin = random.nextFloat() * 720f - 360f,
                color = colors[it % colors.size],
            )
        }
    }
    val progress = remember { Animatable(0f) }
    val finished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        if (!MotionPreferences.reduceMotion) progress.animateTo(1f, tween(ConfettiMillis, easing = LinearEasing))
        finished()
    }
    Canvas(modifier) {
        val t = progress.value
        if (t <= 0f || t >= 1f) return@Canvas
        val piece = Size(6.dp.toPx(), 10.dp.toPx())
        pieces.forEach { p ->
            // Thrown up from the card's middle, then falling under "gravity".
            val x = size.width * (p.x + p.vx * t)
            val y = size.height * (LaunchHeight - p.vy * t + Gravity * t * t)
            rotate(p.spin * t, pivot = Offset(x, y)) {
                drawRect(p.color, topLeft = Offset(x, y), size = piece, alpha = 1f - t)
            }
        }
    }
}
