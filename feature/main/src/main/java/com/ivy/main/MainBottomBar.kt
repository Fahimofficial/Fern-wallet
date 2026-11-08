package com.ivy.main

import androidx.annotation.DrawableRes
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.ivy.design.system.motion.MotionPreferences
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.ivy.legacy.utils.drawColoredShadow
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
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.wallet.ui.theme.gradientIncome
import com.ivy.wallet.ui.theme.paletteGradient
import com.ivy.wallet.ui.theme.GradientTransfer
import com.ivy.wallet.ui.theme.components.IvyCircleButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.components.IvyOutlinedButton
import com.ivy.wallet.ui.theme.gradientExpenses
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
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
private const val NavBarZIndex = 50f
private const val TabIconPressedScale = 0.8f

// Covers the + rising 34dp above the bar, so list text fades out before it reaches it.
private val NavScrimHeight = 40.dp

// What the FAB does right now, for screen readers.
private fun fabLabel(tab: MainTab, expanded: Boolean): Int = when {
    tab == MainTab.ACCOUNTS -> R.string.add_account
    expanded -> R.string.close
    else -> R.string.add_transaction
}

// Apple's rubber-band curve: the further past the rest position, the less the element follows,
// so a pull reads as "responsive, but tethered" instead of stopping hard.
private fun rubberBand(distance: Float, range: Float, constant: Float = 0.55f): Float =
    distance * range * constant / (range + constant * abs(distance))

val TRN_BUTTON_CLICK_AREA_HEIGHT = 150.dp
val FAB_BUTTON_SIZE = 56.dp

/** @param initiallyExpanded only used by screenshot tests to render the add menu open. */
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("LongMethod", "CyclomaticComplexMethod")
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
    barsVisible: Boolean = true,
    fabExtended: Boolean = false,
    onFabLabelSeen: () -> Unit = {},
) {
    val ivyContext = ivyWalletCtx()

    var expanded by remember { mutableStateOf(initiallyExpanded) }

    // Hidden while scrolling down Home; never while the add menu is open or TalkBack is on
    // (a screen-reader user can't scroll up to bring back controls they can't see).
    val touchExploration = rememberTouchExplorationEnabled()
    val hideProgress = animateFloatAsState(
        targetValue = if (!barsVisible && !expanded && !touchExploration) 1f else 0f,
        animationSpec = IvyMotion.standard(IvyMotion.Duration.Medium),
        label = "bars_hide",
    )

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

    val navBarColor = FernTheme.palette[ColorToken.NavBar]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.BottomCenter)
            // Above the tabs (zIndex 1), below their sheets and menus (500+).
            .zIndex(NavBarZIndex)
            .graphicsLayer { translationY = hideProgress.value * (size.height + NavScrimHeight.toPx()) }
            // Content scrolling under the bar fades out instead of meeting it at a hard edge.
            .drawBehind {
                val scrim = NavScrimHeight.toPx()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, navBarColor),
                        startY = -scrim,
                        endY = 0f,
                    ),
                    topLeft = Offset(0f, -scrim),
                    size = Size(size.width, scrim),
                )
            }
            // Opaque: at 95% the list's text showed through the tab labels.
            .background(navBarColor)
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
            selectedColor = FernTheme.palette[ColorToken.NavSelected]
        ) {
            selectTab(MainTab.HOME)
        }

        Spacer(Modifier.width(FAB_BUTTON_SIZE))

        Tab(
            icon = R.drawable.ic_accounts,
            name = stringResource(R.string.accounts),
            selected = tab == MainTab.ACCOUNTS,
            selectedColor = FernTheme.palette[ColorToken.NavSelected]
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
    // + & x button. A pill that shows "Add" in the first session, then a round button.
    val fabInteraction = remember { MutableInteractionSource() }
    val fabGradient = if (tab == MainTab.HOME && expanded) {
        Gradient.solid(UI.colors.gray)
    } else {
        paletteGradient(ColorToken.Fab)
    }
    val fabTint = if (expanded) UI.colors.gray.contentColor() else FernTheme.palette[ColorToken.Fab].contentColor()
    val fabDescription = stringResource(fabLabel(tab, expanded))
    val fabHiddenOffset = ivyContext.screenHeight - fabStartY
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    placeable.place(
                        // Centred on its own width, so the pill grows to both sides.
                        x = (ivyContext.screenWidth - placeable.width) / 2,
                        y = fabStartY.roundToInt()
                    )
                }
            }
            .height(FAB_BUTTON_SIZE)
            .widthIn(min = FAB_BUTTON_SIZE)
            .graphicsLayer {
                translationX = fabPull.value.x
                translationY = fabPull.value.y + hideProgress.value * fabHiddenOffset
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
            .pressScale(fabInteraction)
            .thenIf(!expanded) {
                drawColoredShadow(
                    color = fabGradient.startColor,
                    borderRadius = 0.dp,
                    shadowRadius = 16.dp,
                    offsetX = 0.dp,
                    offsetY = 8.dp
                )
            }
            .clip(UI.shapes.rFull)
            .background(fabGradient.asHorizontalBrush(), UI.shapes.rFull)
            .clickable(
                interactionSource = fabInteraction,
                indication = LocalIndication.current,
                role = Role.Button,
            ) {
                onFabLabelSeen()
                when (tab) {
                    MainTab.HOME -> {
                        expanded = !expanded
                    }

                    MainTab.ACCOUNTS -> {
                        showAddAccountModal()
                    }
                }
            }
            .semantics { contentDescription = fabDescription }
            .testTag("fab_add")
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IvyIcon(
            modifier = Modifier
                .size(FAB_BUTTON_SIZE - 16.dp)
                .graphicsLayer { rotationZ = fabRotation.value },
            icon = R.drawable.ic_add,
            tint = fabTint,
        )
        AnimatedVisibility(
            visible = fabExtended && tab == MainTab.HOME && !expanded,
            enter = fadeIn(IvyMotion.standard(IvyMotion.Duration.Medium)) +
                expandHorizontally(IvyMotion.standard(IvyMotion.Duration.Medium)),
            exit = fadeOut(IvyMotion.standard(IvyMotion.Duration.Short)) +
                shrinkHorizontally(IvyMotion.standard(IvyMotion.Duration.Medium)),
            label = "fab_label",
        ) {
            Text(
                // The button's description already says what it does.
                modifier = Modifier
                    .padding(end = 16.dp)
                    .clearAndSetSemantics { },
                text = stringResource(R.string.home_add_short),
                style = UI.typo.b2.style(fontWeight = FontWeight.ExtraBold, color = fabTint),
            )
        }
    }
}

@Composable
private fun rememberTouchExplorationEnabled(): Boolean {
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(AccessibilityManager::class.java) }
    var enabled by remember(manager) { mutableStateOf(manager?.isTouchExplorationEnabled == true) }
    DisposableEffect(manager) {
        val listener = AccessibilityManager.TouchExplorationStateChangeListener { enabled = it }
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose { manager?.removeTouchExplorationStateChangeListener(listener) }
    }
    return enabled
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
        contentDescription = stringResource(R.string.add_income),
        backgroundGradient = gradientIncome(),
        tint = FernTheme.palette[ColorToken.Income].contentColor(),
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
            .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
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
        contentDescription = stringResource(R.string.add_expense),
        backgroundGradient = gradientExpenses(),
        horizontalGradient = false,
        tint = FernTheme.palette[ColorToken.Expense].contentColor(),
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
            .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
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
        contentDescription = stringResource(R.string.transfer),
        backgroundGradient = GradientTransfer,
        tint = FernTheme.palette[ColorToken.Transfer].contentColor(),
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
            .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
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
        targetValue = if (selected) selectedColor else FernTheme.palette[ColorToken.NavUnselected],
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
        // A small spring bounce when this tab becomes selected (not on first show).
        val iconScale = remember { Animatable(1f) }
        var wasSelected by remember { mutableStateOf(selected) }
        LaunchedEffect(selected) {
            if (selected && !wasSelected && !MotionPreferences.reduceMotion) {
                iconScale.snapTo(TabIconPressedScale)
                iconScale.animateTo(1f, IvyMotion.Springs.bouncy())
            }
            wasSelected = selected
        }
        IvyIcon(
            modifier = Modifier.graphicsLayer {
                scaleX = iconScale.value
                scaleY = iconScale.value
            },
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
