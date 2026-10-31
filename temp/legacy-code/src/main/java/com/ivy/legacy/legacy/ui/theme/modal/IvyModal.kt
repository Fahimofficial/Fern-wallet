package com.ivy.wallet.ui.theme.modal

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.design.l0_system.UI
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.utils.rememberInteractionSource
import com.ivy.design.utils.thenIf
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.addKeyboardListener
import com.ivy.legacy.utils.consumeClicks
import com.ivy.legacy.utils.densityScope
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.keyboardOnlyWindowInsets
import com.ivy.legacy.utils.navigationBarInsets
import com.ivy.legacy.utils.onScreenStart
import com.ivy.navigation.Navigation
import com.ivy.navigation.navigation
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.CloseButton
import com.ivy.wallet.ui.theme.gradientCutBackgroundTop
import com.ivy.wallet.ui.theme.mediumBlur
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val DURATION_MODAL_ANIM = 200

/** Pull the sheet (or flick it so it would travel) this far down and releasing dismisses it. */
private val DismissDistance = 160.dp
private val GrabberWidth = 32.dp
private val GrabberHeight = 4.dp
private val GrabberTop = 8.dp

/**
 * Bottom sheet used by every legacy modal.
 *
 * Motion: one spring-driven value moves the sheet, its actions row and the scrim together, so
 * they can never drift apart (the scrim used to fade for 400 ms while the sheet slid for 200 ms).
 * The sheet can be dragged down 1:1 with the finger once its content is scrolled to the top;
 * on release the flick's projected landing point decides between dismissing and settling back,
 * and the spring continues at the finger's velocity. Every animation starts from the value on
 * screen, so reopening mid-close (or grabbing mid-settle) never jumps.
 */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxScope.IvyModal(
    id: UUID?,
    visible: Boolean,
    dismiss: () -> Unit,
    SecondaryActions: (@Composable () -> Unit)? = null,
    PrimaryAction: @Composable () -> Unit,
    scrollState: ScrollState? = rememberScrollState(),
    shiftIfKeyboardShown: Boolean = true,
    includeActionsRowPadding: Boolean = true,
    Content: @Composable ColumnScope.() -> Unit
) {
    val rootView = LocalView.current
    var keyboardShown by remember { mutableStateOf(false) }

    onScreenStart {
        rootView.addKeyboardListener {
            keyboardShown = it
        }
    }

    val keyboardShownInsetDp by animateDpAsState(
        targetValue = densityScope {
            if (keyboardShown) keyboardOnlyWindowInsets().bottom.toDp() else 0.dp
        },
        animationSpec = tween(DURATION_MODAL_ANIM)
    )
    val navBarPadding by animateDpAsState(
        targetValue = densityScope {
            if (keyboardShown) 0.dp else navigationBarInsets().bottom.toDp()
        },
        animationSpec = tween(DURATION_MODAL_ANIM)
    )

    val latestVisible by rememberUpdatedState(visible)
    val latestDismiss by rememberUpdatedState(dismiss)
    val scope = rememberCoroutineScope()
    val dismissDistancePx = with(LocalDensity.current) { DismissDistance.toPx() }
    val sheet = remember { SheetMotion(initiallyVisible = visible, scope = scope) }
    sheet.dismissDistancePx = dismissDistancePx
    sheet.isVisible = { latestVisible }
    sheet.onDismiss = {
        hideKeyboard(rootView)
        latestDismiss()
    }

    LaunchedEffect(visible) {
        sheet.animateVisibility(visible)
    }

    val onScreen by remember { derivedStateOf { sheet.progress.value > 0f } }

    if (visible || onScreen) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = sheet.scrimAlpha() }
                .background(mediumBlur())
                .testTag("modal_outside_blur")
                .clickable(
                    onClick = {
                        hideKeyboard(rootView)
                        dismiss()
                    },
                    enabled = visible
                )
                .zIndex(1000f)
        )

        var actionsRowHeight by remember { mutableIntStateOf(0) }
        val grabberColor = UI.colors.gray

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { sheet.sheetHeightPx = it.height }
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        // Read in the layout phase: moving the sheet never recomposes it.
                        placeable.placeRelative(0, sheet.offsetPx(placeable.height).roundToInt())
                    }
                }
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 24.dp)
                .background(UI.colors.pure, UI.shapes.r2Top)
                .drawWithContent {
                    drawContent()
                    // Grabber: tells the user the sheet can be pulled down.
                    val size = Size(GrabberWidth.toPx(), GrabberHeight.toPx())
                    drawRoundRect(
                        color = grabberColor,
                        topLeft = Offset((this.size.width - size.width) / 2f, GrabberTop.toPx()),
                        size = size,
                        cornerRadius = CornerRadius(size.height / 2f),
                        alpha = GrabberAlpha,
                    )
                }
                .consumeClicks(rememberInteractionSource())
                .nestedScroll(sheet.dragConnection)
                .thenIf(scrollState != null) {
                    verticalScroll(scrollState!!)
                }
                .zIndex(1000f)
        ) {
            ModalBackHandling(
                modalId = id,
                visible = visible,
                dismiss = dismiss
            )

            Content()

            // Bottom padding
            if (includeActionsRowPadding) {
                Spacer(Modifier.height(densityScope { actionsRowHeight.toDp() }))
            }

            if (shiftIfKeyboardShown) {
                Spacer(Modifier.height(keyboardShownInsetDp))
            }
        }

        ModalActionsRow(
            sheet = sheet,
            keyboardShownInsetDp = keyboardShownInsetDp,
            navBarPadding = navBarPadding,
            onHeightChanged = {
                actionsRowHeight = it
            },
            onClose = {
                hideKeyboard(rootView)
                dismiss()
            },
            SecondaryActions = SecondaryActions,
            PrimaryAction = PrimaryAction
        )
    }
}

private const val GrabberAlpha = 0.4f

/**
 * Sheet position: [progress] (0 hidden, 1 shown) for opening and closing, plus [dragPx], how far
 * the user has pulled the sheet down. Both are read only in layout/draw lambdas.
 */
@Stable
private class SheetMotion(initiallyVisible: Boolean, private val scope: CoroutineScope) {
    val progress = Animatable(if (initiallyVisible) 1f else 0f)
    var dragPx by mutableFloatStateOf(0f)
        private set
    var sheetHeightPx by mutableIntStateOf(0)
    var dismissDistancePx = 0f
    var isVisible: () -> Boolean = { initiallyVisible }
    var onDismiss: () -> Unit = {}
    private var settleJob: Job? = null

    fun offsetPx(height: Int): Float = height * (1f - progress.value) + dragPx

    fun scrimAlpha(): Float {
        val height = sheetHeightPx
        val dragFraction = if (height > 0) (dragPx / height).coerceIn(0f, 1f) else 0f
        return progress.value.coerceIn(0f, 1f) * (1f - dragFraction)
    }

    suspend fun animateVisibility(visible: Boolean) {
        if (visible) {
            if (dragPx != 0f) scope.launch { settleDrag(target = 0f, velocity = 0f) }
            progress.animateTo(1f, IvyMotion.Springs.surface(), initialVelocity = progress.velocity)
        } else {
            progress.animateTo(0f, IvyMotion.Springs.surface(), initialVelocity = progress.velocity)
            settleJob?.cancel()
            dragPx = 0f
        }
    }

    private fun grab() {
        settleJob?.cancel()
        settleJob = null
    }

    private suspend fun settleDrag(target: Float, velocity: Float) {
        animate(
            initialValue = dragPx,
            targetValue = target,
            initialVelocity = velocity,
            animationSpec = IvyMotion.Springs.surface(),
        ) { value, _ -> dragPx = value }
    }

    private fun release(velocity: Float) {
        val height = sheetHeightPx.toFloat()
        val projected = dragPx + IvyMotion.projectedDistance(velocity)
        val flickedUp = velocity < 0f
        val dismiss = height > 0f && !flickedUp && projected > min(dismissDistancePx, height / 2f)
        settleJob = scope.launch {
            if (dismiss) {
                // Hand the drag over to the closing spring, keeping the finger's velocity.
                progress.snapTo((1f - dragPx / height).coerceIn(0f, 1f))
                dragPx = 0f
                onDismiss()
                progress.animateTo(0f, IvyMotion.Springs.surface(), initialVelocity = -velocity / height)
                // The caller didn't hide the modal (e.g. it can't be dismissed now): bring it back.
                if (isVisible()) progress.animateTo(1f, IvyMotion.Springs.surface())
            } else {
                settleDrag(target = 0f, velocity = velocity)
            }
        }
    }

    val dragConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            // Pushing back up first lifts the pulled-down sheet, then scrolls the content.
            if (source != NestedScrollSource.UserInput || available.y >= 0f || dragPx <= 0f) {
                return Offset.Zero
            }
            grab()
            val consumed = max(available.y, -dragPx)
            dragPx += consumed
            return Offset(0f, consumed)
        }

        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            // Content is at its top and the finger keeps pulling down: the sheet follows 1:1.
            if (source != NestedScrollSource.UserInput || available.y <= 0f || !isVisible()) {
                return Offset.Zero
            }
            grab()
            dragPx += available.y
            return Offset(0f, available.y)
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (dragPx <= 0f) return Velocity.Zero
            release(available.y)
            return available
        }
    }
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
private fun ModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    dismiss: () -> Unit
) {
    AddModalBackHandling(
        modalId = modalId,
        visible = visible,
        action = {
            dismiss()
        }
    )
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun AddModalBackHandling(
    modalId: UUID?,
    visible: Boolean,
    action: () -> Unit
) {
    val latestAction by rememberUpdatedState(action)
    val nav = navigation()
    DisposableEffect(visible) {
        if (visible) {
            val lastModalBackHandlingId = nav.lastModalBackHandlerId()

            if (modalId != null && modalId != lastModalBackHandlingId) {
                nav.modalBackHandling.add(
                    Navigation.ModalBackHandler(
                        id = modalId,
                        onBackPressed = {
                            if (visible) {
                                latestAction()
                                true
                            } else {
                                false
                            }
                        }
                    )
                )
            }
        }

        onDispose {
            val lastModalBackHandlingId = nav.lastModalBackHandlerId()
            if (modalId != null && lastModalBackHandlingId == modalId) {
                removeLastBackHandlerSafe(nav)
            }
        }
    }
}

private fun removeLastBackHandlerSafe(nav: Navigation) {
    if (nav.modalBackHandling.isNotEmpty()) {
        nav.modalBackHandling.pop()
    }
}

/** The close/primary actions row. It belongs to the sheet, so it moves with the sheet's offset. */
@Suppress("ParameterNaming")
@Composable
private fun ModalActionsRow(
    sheet: SheetMotion,
    keyboardShownInsetDp: Dp,
    navBarPadding: Dp,

    onHeightChanged: (Int) -> Unit,

    onClose: () -> Unit,
    SecondaryActions: (@Composable () -> Unit)? = null,
    PrimaryAction: @Composable () -> Unit
) {
    val ivyContext = ivyWalletCtx()
    ActionsRow(
        modifier = Modifier
            .onSizeChanged {
                onHeightChanged(it.height)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val systemOffsetBottom = keyboardShownInsetDp.toPx()
                val sheetOffset = sheet.offsetPx(sheet.sheetHeightPx)
                val y = ivyContext.screenHeight - placeable.height - systemOffsetBottom + sheetOffset

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        0,
                        y.roundToInt()
                    )
                }
            }
            .gradientCutBackgroundTop(
                pure = UI.colors.pure,
                density = LocalDensity.current,
                endY = 16.dp
            )
            .padding(top = 8.dp, bottom = 12.dp)
            .padding(bottom = navBarPadding)
            .zIndex(1100f)
    ) {
        Spacer(Modifier.width(24.dp))

        CloseButton(
            modifier = Modifier.testTag("modal_close_button"),
            onClick = onClose
        )

        SecondaryActions?.invoke()

        Spacer(Modifier.weight(1f))

        PrimaryAction()

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun PreviewIvyModal_minimal() {
    IvyWalletPreview {
        IvyModal(
            id = UUID.randomUUID(),
            visible = true,
            PrimaryAction = {
                ModalSave {
                }
            },
            dismiss = {}
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                modifier = Modifier.padding(horizontal = 24.dp),
                text = "My first Ivy Modal"
            )

            ModalPreviewActionRowSpacer()
        }
    }
}

@Composable
fun ModalPreviewActionRowSpacer() {
    Spacer(Modifier.height(modalPreviewActionRowHeight()))
}

@Composable
fun modalPreviewActionRowHeight() = 80.dp
