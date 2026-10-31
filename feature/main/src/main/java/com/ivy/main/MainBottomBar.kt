package com.ivy.main

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.utils.thenIf
import com.ivy.home.HomeSampleUi
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.lerp
import com.ivy.legacy.utils.navigationBarInset
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.springBounceFast
import com.ivy.legacy.utils.toDensityDp
import com.ivy.legacy.utils.toDensityPx
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.GradientGreen
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.GradientTransfer
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.gradientExpenses
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import com.ivy.wallet.ui.theme.pureBlur
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.design.system.motion.IvyMotion
import com.ivy.design.system.motion.pressScale
import kotlinx.coroutines.launch

private const val MenuVisibleThreshold = 0.01f

/**
 * Apple's rubber-band curve: the further past the rest position, the less the element follows,
 * so a pull reads as "responsive, but tethered" instead of stopping hard.
 */
private fun rubberBand(distance: Float, range: Float, constant: Float = 0.55f): Float =
    distance * range * constant / (range + constant * abs(distance))

val TRN_BUTTON_CLICK_AREA_HEIGHT = 150.dp
val FAB_BUTTON_SIZE = 56.dp

/** @param initiallyExpanded only used by screenshot tests to render the add menu open. */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("LongMethod")
@Composable
fun BoxWithConstraintsScope.BottomBar(
    tab: MainTab,
    selectTab: (MainTab) -> Unit,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,

    showAddAccountModal: () -> Unit,
    initiallyExpanded: Boolean = false,
) {
    val ivyContext = ivyWalletCtx()

    var expanded by remember { mutableStateOf(initiallyExpanded) }

    val modalId = remember { UUID.randomUUID() }

    AddModalBackHandling(
        modalId = modalId,
        visible = expanded
    ) {
        expanded = false
    }

    // Animated values are kept as State and read only inside layout/draw lambdas, so the menu
    // opening moves and fades things without recomposing the bar on every frame.
    val fabRotation = animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = springBounceFast(),
        label = "fab_rotation",
    )

    val buttonsShown = animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = springBounceFast(),
        label = "fab_menu",
    )
    val menuOnScreen by remember { derivedStateOf { buttonsShown.value > MenuVisibleThreshold } }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            .background(pureBlur())
            .graphicsLayer { alpha = (1f - buttonsShown.value).coerceIn(0f, 1f) }
            .navigationBarsPadding()
            .clickableNoIndication(rememberInteractionSource()) {
                // consume click
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Tab(
            icon = R.drawable.ic_home,
            name = stringResource(R.string.home),
            selected = tab == MainTab.HOME,
            selectedColor = Ivy
        ) {
            selectTab(MainTab.HOME)
        }

        Spacer(Modifier.width(FAB_BUTTON_SIZE))

        Tab(
            icon = R.drawable.ic_accounts,
            name = stringResource(R.string.accounts),
            selected = tab == MainTab.ACCOUNTS,
            selectedColor = Ivy
        ) {
            selectTab(MainTab.ACCOUNTS)
        }
    }

    if (menuOnScreen) {
        // The backdrop fades in place (it used to sweep the whole screen up) while the actions
        // fly out of the FAB, so the motion stays anchored to the button that opened it.
        Spacer(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = buttonsShown.value.coerceIn(0f, 1f) }
                .background(UI.colors.pure.copy(alpha = 0.95f))
                .clickableNoIndication(rememberInteractionSource()) {
                    // Tapping outside the actions closes the menu, like every other overlay.
                    expanded = false
                }
                .zIndex(100f)
        )
    }

    // ------------------------------------ BUTTONS--------------------------------------------------
    val fabStartX = ivyContext.screenWidth / 2 - FAB_BUTTON_SIZE.toDensityPx() / 2
    val fabStartY = ivyContext.screenHeight - navigationBarInset() -
            30.dp.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

    TransactionButtons(
        visible = menuOnScreen,
        buttonsShownPercent = { buttonsShown.value },

        fabStartX = fabStartX,
        fabStartY = fabStartY,

        onAddIncome = onAddIncome,
        onAddExpense = onAddExpense,
        onAddTransfer = onAddTransfer,
        onAddPlannedPayment = onAddPlannedPayment
    )

    var dragOffset by remember {
        mutableStateOf(Offset.Zero)
    }
    // Where the FAB is drawn while the user swipes it: it follows the finger with resistance,
    // so the swipe shortcuts (up: expense, up-left: income, up-right: transfer) feel physical.
    val fabPull = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    var swipeCommitted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val fabPullRangePx = FAB_BUTTON_SIZE.toDensityPx()
    // + & x button
    IvyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = fabStartX.roundToInt(),
                        y = fabStartY.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .graphicsLayer {
                rotationZ = fabRotation.value
                translationX = fabPull.value.x
                translationY = fabPull.value.y
            }
            .zIndex(200f)
            .thenIf(tab == MainTab.HOME) {
                pointerInput(Unit) {
                    fun settle() {
                        dragOffset = Offset.Zero
                        swipeCommitted = false
                        scope.launch { fabPull.animateTo(Offset.Zero, IvyMotion.Springs.release()) }
                    }
                    detectDragGestures(
                        onDragCancel = { settle() },
                        onDragEnd = { settle() },
                        onDrag = { _, dragAmount ->
                            if (swipeCommitted) return@detectDragGestures
                            dragOffset += dragAmount
                            val pull = Offset(
                                x = rubberBand(dragOffset.x, fabPullRangePx),
                                y = rubberBand(dragOffset.y, fabPullRangePx),
                            )
                            scope.launch { fabPull.snapTo(pull) }

                            val horizontalThreshold = 40
                            val verticalThreshold = 60

                            when {
                                abs(dragOffset.x) < horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up
                                    // One action per swipe: ignore the rest of this drag.
                                    swipeCommitted = true
                                    IvyHaptics.gestureCommit(view)
                                    onAddExpense()
                                }

                                dragOffset.x < -horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up left
                                    // One action per swipe: ignore the rest of this drag.
                                    swipeCommitted = true
                                    IvyHaptics.gestureCommit(view)
                                    onAddIncome()
                                }

                                dragOffset.x > horizontalThreshold &&
                                        dragOffset.y < -verticalThreshold -> {
                                    // swipe up right
                                    // One action per swipe: ignore the rest of this drag.
                                    swipeCommitted = true
                                    IvyHaptics.gestureCommit(view)
                                    onAddTransfer()
                                }
                            }
                        }
                    )
                }
            }
            .testTag("fab_add"),
        backgroundPadding = 8.dp,
        icon = R.drawable.ic_add,
        backgroundGradient = when (tab) {
            MainTab.HOME -> {
                if (!expanded) GradientIvy else Gradient.solid(UI.colors.gray)
            }

            // One primary-action color across tabs (was purple on Home, green on Accounts).
            MainTab.ACCOUNTS -> {
                GradientIvy
            }
        },
        hasShadow = !expanded,
        tint = when (tab) {
            MainTab.HOME -> White
            MainTab.ACCOUNTS -> White
        }
    ) {
        when (tab) {
            MainTab.HOME -> {
                expanded = !expanded
            }

            MainTab.ACCOUNTS -> {
                showAddAccountModal()
            }
        }
    }
}

@Composable
private fun TransactionButtons(
    visible: Boolean,
    buttonsShownPercent: () -> Float,

    fabStartX: Float,
    fabStartY: Float,

    onAddIncome: () -> Unit,
    onAddExpense: () -> Unit,
    onAddTransfer: () -> Unit,
    onAddPlannedPayment: () -> Unit,
) {
    val ivyContext = ivyWalletCtx()

    val bH = 48.dp
    val bV = 20.dp // 24.dp
    val bCenterV = 74.dp // 80.dp

    if (visible) {
        val buttonLeftX = bH.toDensityPx()
        val buttonRightX = ivyContext.screenWidth - bH.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

        val sideButtonsY = fabStartY - bV.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()
        val buttonCenterY = fabStartY - bCenterV.toDensityPx() - FAB_BUTTON_SIZE.toDensityPx()

        val clickAreaWidth = ivyContext.screenWidth / 3

        IvyOutlinedButton(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placealbe = measurable.measure(constraints)
                    layout(placealbe.width, placealbe.height) {
                        placealbe.place(
                            x = ivyContext.screenWidth / 2 - placealbe.width / 2,
                            y = buttonCenterY.roundToInt() - 48.dp.roundToPx() - placealbe.height - FAB_BUTTON_SIZE.roundToPx()
                        )
                    }
                }
                .padding(horizontal = 8.dp)
                .graphicsLayer { alpha = buttonsShownPercent().coerceIn(0f, 1f) }
                .zIndex(200f),
            iconStart = R.drawable.ic_planned_payments,
            text = stringResource(R.string.add_planned_payment),
            solidBackground = true
        ) {
            onAddPlannedPayment()
        }

        // Add INCOME ------------------------------------------------------------------------------
        AddIncomeButton(
            buttonsShownPercent = buttonsShownPercent,
            fabStartX = fabStartX,
            fabStartY = fabStartY,
            buttonLeftX = buttonLeftX,
            sideButtonsY = sideButtonsY,
            clickAreaWidth = clickAreaWidth,
            onAddIncome = onAddIncome
        )
        // Add INCOME ------------------------------------------------------------------------------

        // Add EXPENSE ------------------------------------------------------------------------------
        AddExpenseButton(
            buttonsShownPercent = buttonsShownPercent,
            fabStartX = fabStartX,
            fabStartY = fabStartY,
            buttonCenterY = buttonCenterY,
            clickAreaWidth = clickAreaWidth,
            onAddExpense = onAddExpense
        )
        // Add EXPENSE ------------------------------------------------------------------------------

        // Add TRANSFER ----------------------------------------------------------------------------
        AddTransferButton(
            buttonsShownPercent = buttonsShownPercent,
            fabStartX = fabStartX,
            fabStartY = fabStartY,
            buttonRightX = buttonRightX,
            sideButtonsY = sideButtonsY,
            clickAreaWidth = clickAreaWidth,
            onAddTransfer = onAddTransfer
        )
        // Add TRANSFER ----------------------------------------------------------------------------
    }
}

@Composable
private fun AddIncomeButton(
    buttonsShownPercent: () -> Float,
    fabStartX: Float,
    fabStartY: Float,
    buttonLeftX: Float,
    sideButtonsY: Float,
    clickAreaWidth: Int,
    onAddIncome: () -> Unit,
) {
    IvyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val x = lerp(fabStartX, buttonLeftX, buttonsShownPercent())
                val y = lerp(
                    fabStartY,
                    sideButtonsY - FAB_BUTTON_SIZE.roundToPx(),
                    buttonsShownPercent()
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = x.roundToInt(),
                        y = y.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(200f),
        icon = R.drawable.ic_income,
        backgroundGradient = GradientGreen,
        tint = White,
        onClick = onAddIncome
    )

    Text(
        modifier = Modifier
            .width(FAB_BUTTON_SIZE + 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = buttonLeftX.roundToInt() - 8.dp.roundToPx(),
                        y = (sideButtonsY + 12.dp.toPx()).roundToInt()
                    )
                }
            }
            .graphicsLayer { alpha = buttonsShownPercent().coerceIn(0f, 1f) }
            .clickableNoIndication(rememberInteractionSource()) {
                onAddIncome()
            }
            .zIndex(200f),
        text = stringResource(R.string.add_income_uppercase),
        style = UI.typo.c.style(
            color = UI.colors.pureInverse,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )

    // Click area
    Spacer(
        modifier = Modifier
            .size(
                width = clickAreaWidth.toDensityDp(),
                height = TRN_BUTTON_CLICK_AREA_HEIGHT
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = 0,
                        y = sideButtonsY.roundToInt() - FAB_BUTTON_SIZE.roundToPx() - 16.dp.roundToPx()
                    )
                }
            }
            .zIndex(199f)
            .clickableNoIndication(rememberInteractionSource()) {
                onAddIncome()
            }
    )
}

@Composable
private fun AddExpenseButton(
    buttonsShownPercent: () -> Float,
    fabStartX: Float,
    fabStartY: Float,
    buttonCenterY: Float,
    clickAreaWidth: Int,
    onAddExpense: () -> Unit,
) {
    IvyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val y =
                    lerp(
                        fabStartY,
                        buttonCenterY - FAB_BUTTON_SIZE.roundToPx(),
                        buttonsShownPercent()
                    )

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = fabStartX.roundToInt(),
                        y = y.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(200f),
        icon = R.drawable.ic_expense,
        backgroundGradient = gradientExpenses(),
        horizontalGradient = false,
        tint = White,
        onClick = onAddExpense
    )

    Text(
        modifier = Modifier
            .width(FAB_BUTTON_SIZE + 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = fabStartX.roundToInt() - 8.dp.roundToPx(),
                        y = (buttonCenterY + 12.dp.toPx()).roundToInt()
                    )
                }
            }
            .graphicsLayer { alpha = buttonsShownPercent().coerceIn(0f, 1f) }
            .clickableNoIndication(rememberInteractionSource()) {
                onAddExpense()
            }
            .zIndex(200f),
        text = stringResource(R.string.add_expense_uppercase),
        style = UI.typo.c.style(
            color = UI.colors.pureInverse,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )

    // Click area
    Spacer(
        modifier = Modifier
            .size(
                width = clickAreaWidth.toDensityDp(),
                height = TRN_BUTTON_CLICK_AREA_HEIGHT
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = clickAreaWidth,
                        y = buttonCenterY.roundToInt() - FAB_BUTTON_SIZE.roundToPx() - 16.dp.roundToPx()
                    )
                }
            }
            .zIndex(199f)
            .clickableNoIndication(rememberInteractionSource()) {
                onAddExpense()
            }
    )
}

@Composable
private fun AddTransferButton(
    buttonsShownPercent: () -> Float,
    fabStartX: Float,
    fabStartY: Float,
    buttonRightX: Float,
    sideButtonsY: Float,
    clickAreaWidth: Int,
    onAddTransfer: () -> Unit,
) {
    IvyCircleButton(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val x = lerp(fabStartX, buttonRightX, buttonsShownPercent())
                val y = lerp(
                    fabStartY,
                    sideButtonsY - FAB_BUTTON_SIZE.roundToPx(),
                    buttonsShownPercent()
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = x.roundToInt(),
                        y = y.roundToInt()
                    )
                }
            }
            .size(FAB_BUTTON_SIZE)
            .zIndex(200f),
        icon = R.drawable.ic_transfer,
        backgroundGradient = GradientTransfer,
        tint = White,
        onClick = onAddTransfer
    )

    Text(
        modifier = Modifier
            .width(FAB_BUTTON_SIZE + 16.dp)
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = buttonRightX.roundToInt() - 8.dp.roundToPx(),
                        y = (sideButtonsY + 12.dp.toPx()).roundToInt()
                    )
                }
            }
            .graphicsLayer { alpha = buttonsShownPercent().coerceIn(0f, 1f) }
            .clickableNoIndication(rememberInteractionSource()) {
                onAddTransfer()
            }
            .zIndex(200f),
        text = stringResource(R.string.account_transfer),
        style = UI.typo.c.style(
            color = UI.colors.pureInverse,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    )

    // Click area
    Spacer(
        modifier = Modifier
            .size(
                width = clickAreaWidth.toDensityDp(),
                height = TRN_BUTTON_CLICK_AREA_HEIGHT
            )
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        x = 2 * clickAreaWidth,
                        y = sideButtonsY.roundToInt() - FAB_BUTTON_SIZE.roundToPx() - 16.dp.roundToPx()
                    )
                }
            }
            .zIndex(199f)
            .clickableNoIndication(rememberInteractionSource()) {
                onAddTransfer()
            }
    )
}

@Composable
private fun RowScope.Tab(
    @DrawableRes icon: Int,
    name: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint by animateColorAsState(
        targetValue = if (selected) selectedColor else UI.colors.pureInverse,
        animationSpec = IvyMotion.standard(IvyMotion.Duration.Short),
        label = "tab_tint",
    )
    Row(
        modifier = Modifier
            .weight(1f)
            .pressScale(interactionSource)
            .clip(UI.shapes.rFull)
            // Announced as "<name>, Tab, Selected/Not selected"; descendants are merged.
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Tab,
                onClick = onClick,
            )
            .padding(top = 12.dp, bottom = 16.dp)
            .testTag(name.lowercase()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IvyIcon(
            icon = icon,
            tint = tint,
            // The label text is only visible when selected; otherwise the icon carries the name.
            contentDescription = if (selected) null else name
        )

        // The label grows out of the icon instead of popping in and shifting the icon sideways.
        AnimatedVisibility(
            visible = selected,
            enter = fadeIn(IvyMotion.standard(IvyMotion.Duration.Medium)) +
                expandHorizontally(IvyMotion.standard(IvyMotion.Duration.Medium)),
            exit = fadeOut(IvyMotion.standard(IvyMotion.Duration.Short)) +
                shrinkHorizontally(IvyMotion.standard(IvyMotion.Duration.Medium)),
            label = "tab_label",
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = name,
                    style = UI.typo.c.style(
                        fontWeight = FontWeight.Bold,
                        color = selectedColor
                    )
                )
            }
        }
    }
}

/** For screenshot testing: Home with the add-transaction menu expanded (sample data). */
@Composable
fun MainFabMenuUiTest(isDark: Boolean) {
    IvyWalletPreview(if (isDark) Theme.DARK else Theme.LIGHT) {
        HomeSampleUi()
        BottomBar(
            tab = MainTab.HOME,
            selectTab = {},
            onAddIncome = {},
            onAddExpense = {},
            onAddTransfer = {},
            onAddPlannedPayment = {},
            showAddAccountModal = {},
            initiallyExpanded = true,
        )
    }
}
