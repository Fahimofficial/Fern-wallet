package com.ivy.ui.undo

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.AccessibilityManager
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.pressScale
import com.ivy.ui.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val VisibleMillis = 5_000L

// Clears the bottom bar and its FAB on the main screen.
private val BottomClearance = 96.dp

private val Raleway = FontFamily(
    Font(R.font.raleway_bold, FontWeight.Bold),
    Font(R.font.raleway_extrabold, FontWeight.ExtraBold),
)

/**
 * Shows [UndoController.current] near the bottom of the screen. Place it once, above all screens.
 *
 * It rises from the bottom edge and leaves the same way, can be flicked sideways to dismiss (it
 * follows the finger and keeps the release velocity), and disappears on its own after a timeout
 * that accessibility services may lengthen.
 */
@Composable
fun UndoSnackbarHost(
    controller: UndoController,
    modifier: Modifier = Modifier,
) {
    // Outlives each message, so an Undo still runs to completion while its snackbar animates out.
    val hostScope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, bottom = BottomClearance),
        contentAlignment = Alignment.BottomCenter,
    ) {
        AnimatedContent(
            targetState = controller.current,
            contentKey = { it?.id },
            transitionSpec = { riseAndFall() },
            label = "undo_snackbar",
        ) { message ->
            if (message != null) {
                UndoSnackbar(
                    message = message,
                    onTimeout = { controller.dismiss(message) },
                    onSwipedAway = { controller.dismiss(message) },
                    onUndo = {
                        hostScope.launch { message.onUndo() }
                        controller.dismiss(message)
                    },
                )
            }
        }
    }
}

private fun riseAndFall(): ContentTransform = ContentTransform(
    targetContentEnter = slideInVertically(IvyMotion.Springs.surface()) { it } +
        fadeIn(IvyMotion.standard(IvyMotion.Duration.Short)),
    initialContentExit = slideOutVertically(IvyMotion.Springs.surface()) { it } +
        fadeOut(IvyMotion.standard(IvyMotion.Duration.Short)),
    sizeTransform = SizeTransform(clip = false),
)

@Composable
private fun UndoSnackbar(
    message: UndoMessage,
    onTimeout: () -> Unit,
    onSwipedAway: () -> Unit,
    onUndo: () -> Unit,
) {
    val accessibility = LocalAccessibilityManager.current
    LaunchedEffect(message.id) {
        delay(accessibleTimeout(accessibility))
        onTimeout()
    }

    val scope = rememberCoroutineScope()
    val swipe = remember(scope) { SwipeDismissState(scope) }
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .widthIn(max = 560.dp)
            .fillMaxWidth()
            .swipeToDismiss(swipe, onSwipedAway)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.inverseSurface)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(message.text),
            style = TextStyle(
                fontFamily = Raleway,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.inverseOnSurface,
            ),
        )

        Spacer(Modifier.width(12.dp))

        UndoButton(label = stringResource(message.actionLabel), onClick = onUndo)
    }
}

// How long the message stays; accessibility services may ask for longer.
private fun accessibleTimeout(accessibility: AccessibilityManager?): Long =
    accessibility?.calculateRecommendedTimeoutMillis(
        originalTimeoutMillis = VisibleMillis,
        containsIcons = false,
        containsText = true,
        containsControls = true,
    ) ?: VisibleMillis

// Horizontal offset of a swipe-dismissable element and the width it travels to leave.
@Stable
private class SwipeDismissState(scope: CoroutineScope) {
    val offsetX = Animatable(0f)
    var widthPx = 0f
    val drag = DraggableState { delta ->
        scope.launch { offsetX.snapTo(offsetX.value + delta) }
    }
}

// Follows a horizontal drag 1:1 and fades as it travels. On release the flick's projected
// landing point decides: past half the width it leaves (keeping the finger's velocity) and
// onDismissed runs; otherwise it springs back.
private fun Modifier.swipeToDismiss(
    state: SwipeDismissState,
    onDismissed: () -> Unit,
): Modifier = this
    .onSizeChanged { state.widthPx = it.width.toFloat() }
    .graphicsLayer {
        val width = state.widthPx
        translationX = state.offsetX.value
        val travelled = if (width > 0f) abs(state.offsetX.value) / width else 0f
        alpha = (1f - travelled).coerceIn(0f, 1f)
    }
    .draggable(
        orientation = Orientation.Horizontal,
        state = state.drag,
        onDragStopped = { velocity ->
            val width = state.widthPx
            val projected = state.offsetX.value + IvyMotion.projectedDistance(velocity)
            if (width > 0f && abs(projected) > width / 2f) {
                val direction = if (projected > 0f) 1f else -1f
                state.offsetX.animateTo(direction * width, IvyMotion.Springs.surface(), velocity)
                onDismissed()
            } else {
                state.offsetX.animateTo(0f, IvyMotion.Springs.surface(), velocity)
            }
        },
    )

@Composable
private fun UndoButton(label: String, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .pressScale(interactionSource)
            .clip(CircleShape)
            .background(colors.primary)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = TextStyle(
                fontFamily = Raleway,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = colors.onPrimary,
            ),
        )
    }
}
