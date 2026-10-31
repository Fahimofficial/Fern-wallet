package com.ivy.wallet.ui.edit.core

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.legacy.datamodel.Account
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.frp.test.TestingContext
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.utils.addKeyboardListener
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.consumeClicks
import com.ivy.legacy.utils.densityScope
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.hideKeyboard
import com.ivy.legacy.utils.lerp
import com.ivy.legacy.utils.onScreenStart
import com.ivy.design.utils.thenIf
import com.ivy.base.model.TransactionType
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.ui.theme.Gradient
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.GreenDark
import com.ivy.wallet.ui.theme.GreenLight
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.IvyDark
import com.ivy.wallet.ui.theme.components.ActionsRow
import com.ivy.wallet.ui.theme.components.BalanceRow
import com.ivy.wallet.ui.theme.components.CircleButton
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon
import com.ivy.wallet.ui.theme.components.IvyButton
import com.ivy.wallet.ui.theme.components.IvyIcon
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.modal.ModalSave
import com.ivy.wallet.ui.theme.modal.ModalSet
import com.ivy.wallet.ui.theme.modal.edit.AmountModal
import com.ivy.wallet.ui.theme.toComposeColor
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalDensity
import com.ivy.design.system.motion.IvyMotion
import com.ivy.legacy.utils.bottomInsetPadding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import androidx.compose.ui.semantics.Role

@Deprecated("Old design system. Use `:ivy-design` and Material3")
const val SWIPE_UP_EXPANDED_THRESHOLD = 200

/** Drag distance for a full expand/collapse until the accounts header has been measured. */
private val FallbackDragRange = 200.dp
private const val HalfExpanded = 0.5f
private const val CollapsedArrowRotation = -180f

/**
 * Expansion progress of the edit sheet (1 = accounts shown). Written synchronously while
 * dragging; spring-animated otherwise, always starting from the current value and velocity.
 */
@Stable
private class SheetExpansion(private val scope: CoroutineScope) {
    var progress by mutableFloatStateOf(1f)
        private set
    private var velocity = 0f
    private var settleJob: Job? = null
    private var dragging = false

    /** Full height of the collapsible header: the drag distance of a full expand/collapse. */
    var rangePx = 0

    fun grab() {
        dragging = true
        settleJob?.cancel()
    }

    fun drag(deltaPx: Float, rangePx: Float) {
        // Dragging down collapses: progress falls as the finger moves down.
        progress = (progress - deltaPx / rangePx).coerceIn(0f, 1f)
    }

    /** @return the state to settle to, chosen from where the flick would land. */
    fun release(velocityPx: Float, rangePx: Float): Float {
        dragging = false
        val projected = progress - IvyMotion.projectedDistance(velocityPx) / rangePx
        val target = if (projected >= HalfExpanded) 1f else 0f
        velocity = -velocityPx / rangePx
        settleAsync(target)
        return target
    }

    fun settleAsync(target: Float) {
        settleJob?.cancel()
        settleJob = scope.launch { settle(target) }
    }

    private suspend fun settle(target: Float) {
        if (dragging) return
        animate(
            initialValue = progress,
            targetValue = target,
            initialVelocity = velocity,
            animationSpec = IvyMotion.Springs.surface(),
        ) { value, v ->
            progress = value.coerceIn(0f, 1f)
            velocity = v
        }
        velocity = 0f
    }
}

@Suppress("LongMethod", "LongParameterList", "UnusedParameter", "ParameterNaming")
@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun BoxWithConstraintsScope.EditBottomSheet(
    initialTransactionId: UUID?,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    amount: Double,
    currency: String,
    amountModalShown: Boolean,
    setAmountModalShown: (Boolean) -> Unit,
    ActionButton: @Composable () -> Unit,
    onAmountChanged: (Double) -> Unit,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
    modifier: Modifier = Modifier, // Modifier comes after other parameters
    convertedAmount: Double? = null,
    convertedAmountCurrencyCode: String? = null,
) {
    val rootView = LocalView.current
    var keyboardShown by remember { mutableStateOf(false) }

    onScreenStart {
        rootView.addKeyboardListener {
            keyboardShown = it
        }
    }

    // Followed frame by frame during the IME animation (see InsetsExt).
    val bottomInsets = WindowInsets.ime.union(WindowInsets.navigationBars)

    var bottomBarHeight by remember { mutableIntStateOf(0) }

    var internalExpanded by remember { mutableStateOf(true) }
    val expanded = internalExpanded && !keyboardShown

    // Expanded (accounts visible) <-> collapsed. The sheet follows the finger while dragged and
    // a release continues at the finger's velocity toward the state the flick projects to.
    val scope = rememberCoroutineScope()
    val expansion = remember { SheetExpansion(scope) }
    val fallbackDragRangePx = with(LocalDensity.current) { FallbackDragRange.toPx() }
    LaunchedEffect(expanded) {
        expansion.settleAsync(if (expanded) 1f else 0f)
    }
    val percentExpanded = expansion.progress
    val percentCollapsed = 1f - percentExpanded

    val showConvertedAmountText by remember(convertedAmount) {
        if (type == TransactionType.TRANSFER && convertedAmount != null && convertedAmountCurrencyCode != null) {
            mutableStateOf(
                "${
                    convertedAmount.format(
                        IvyCurrency.getDecimalPlaces(
                            convertedAmountCurrencyCode
                        )
                    )
                } $convertedAmountCurrencyCode"
            )
        } else {
            mutableStateOf(null)
        }
    }

    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 24.dp)
//            .drawColoredShadow(
//                color = UI.colors.mediumInverse,
//                alpha = if (UI.colors.isLight) 0.3f else 0.2f,
//                borderRadius = 24.dp,
//                shadowRadius = 24.dp
//            )
            .border(
                width = 2.dp,
                color = UI.colors.medium,
                shape = UI.shapes.r2Top
            )
            .background(UI.colors.pure, UI.shapes.r2Top)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    expansion.drag(delta, expansion.rangePx.takeIf { it > 0 }?.toFloat() ?: fallbackDragRangePx)
                },
                onDragStarted = {
                    expansion.grab()
                    // Pulling the sheet up while typing means "show the accounts again".
                    if (keyboardShown) hideKeyboard(rootView)
                },
                onDragStopped = { velocity ->
                    val range = expansion.rangePx.takeIf { it > 0 }?.toFloat() ?: fallbackDragRangePx
                    internalExpanded = expansion.release(velocity, range) == 1f
                },
            )
            .consumeClicks(rememberInteractionSource())
    ) {
        // Accounts label
        val label = when (type) {
            TransactionType.INCOME -> stringResource(R.string.add_money_to)
            TransactionType.EXPENSE -> stringResource(R.string.pay_with)
            TransactionType.TRANSFER -> stringResource(R.string.from)
        }

        SheetHeader(
            percentExpanded = percentExpanded,
            onFullHeight = { expansion.rangePx = it },
            label = label,
            type = type,
            accounts = accounts,
            selectedAccount = selectedAccount,
            toAccount = toAccount,
            onSelectedAccountChanged = onSelectedAccountChanged,
            onToAccountChanged = onToAccountChanged,
            onAddNewAccount = onAddNewAccount
        )

        val spacerAboveAmount = lerp(40, 16, percentCollapsed)
        Spacer(Modifier.height(spacerAboveAmount.dp))

        if (type == TransactionType.TRANSFER && percentExpanded < 1f) {
            TransferRowMini(
                percentCollapsed = percentCollapsed,
                fromAccount = selectedAccount,
                toAccount = toAccount,
                onSetExpanded = {
                    internalExpanded = true
                }
            )
        }

        Amount(
            type = type,
            amount = amount,
            currency = currency,
            label = label,
            account = selectedAccount,
            showConvertedAmountText = showConvertedAmountText,
            percentExpanded = percentExpanded,
            onShowAmountModal = {
                setAmountModalShown(true)
            },
            onAccountMiniClick = {
                hideKeyboard(rootView)
                internalExpanded = true
            },
        )

        val lastSpacer = lerp(20f, 8f, percentCollapsed)
        if (lastSpacer > 0) {
            Spacer(Modifier.height(lastSpacer.dp))
        }
//
        // Room for the bottom bar, which already includes the keyboard / navigation bar inset.
        Spacer(Modifier.height(densityScope { bottomBarHeight.toDp() }))
    }

    BottomBar(
        keyboardShown = keyboardShown,
        internalExpanded = internalExpanded,
        setInternalExpanded = {
            internalExpanded = it
        },
        setBottomBarHeight = {
            bottomBarHeight = it
        },

        bottomInsets = bottomInsets,
        expansionProgress = { expansion.progress },

        ActionButton = ActionButton
    )

    val amountModalId = remember(initialTransactionId, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalShown,
        currency = currency,
        initialAmount = amount.takeIf { it > 0 },
        Header = {
            Spacer(Modifier.height(24.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = stringResource(R.string.account),
                style = UI.typo.b1.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "amount_modal_account"
            )
        },
        amountSpacerTop = 48.dp,
        dismiss = {
            setAmountModalShown(false)
        }
    ) {
        onAmountChanged(it)
    }
}

@Composable
private fun BottomBar(
    keyboardShown: Boolean,
    bottomInsets: WindowInsets,
    setBottomBarHeight: (Int) -> Unit,
    internalExpanded: Boolean,
    setInternalExpanded: (Boolean) -> Unit,
    expansionProgress: () -> Float,
    ActionButton: @Composable () -> Unit
) {
    val ivyContext = ivyWalletCtx()

    ActionsRow(
        modifier = Modifier
            .onSizeChanged {
                setBottomBarHeight(it.height)
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                // Bottom-aligned; the inset padding below keeps the buttons above the keyboard.
                val y = ivyContext.screenHeight - placeable.height

                layout(placeable.width, placeable.height) {
                    placeable.place(
                        0,
                        y
                    )
                }
            }
//            .gradientCutBackground()
            .bottomInsetPadding(bottomInsets)
            .padding(bottom = 12.dp),
        lineColor = UI.colors.medium
    ) {
        Spacer(Modifier.width(24.dp))

        val rootView = LocalView.current
        CircleButton(
            // Turns with the sheet, also while it is being dragged.
            modifier = Modifier.graphicsLayer { rotationZ = CollapsedArrowRotation * (1f - expansionProgress()) },
            icon = R.drawable.ic_expand_more,
        ) {
            setInternalExpanded(!internalExpanded || keyboardShown)
            hideKeyboard(rootView)
        }

        Spacer(Modifier.weight(1f))

        ActionButton()

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
@Suppress("ParameterNaming", "MultipleEmitters")
private fun TransferRowMini(
    percentCollapsed: Float,
    fromAccount: Account?,
    toAccount: Account?,
    onSetExpanded: () -> Unit
) {
    Row(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val height = placeable.height * (percentCollapsed)

                layout(placeable.width, height.roundToInt()) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(percentCollapsed)
            .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                onSetExpanded()
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(24.dp))

        val fromColor = fromAccount?.color?.toComposeColor() ?: Ivy
        val fromContrastColor = findContrastTextColor(fromColor)
        IvyButton(
            text = fromAccount?.name ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundGradient = Gradient.solid(fromColor),
            iconTint = fromContrastColor,
            textStyle = UI.typo.b2.style(
                color = fromContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }

        IvyIcon(
            icon = R.drawable.ic_arrow_right,
            tint = UI.colors.pureInverse
        )

        val toColor = toAccount?.color?.toComposeColor() ?: Ivy
        val toContrastColor = findContrastTextColor(toColor)
        IvyButton(
            text = toAccount?.name ?: "Null",
            iconStart = R.drawable.ic_accounts,
            backgroundGradient = Gradient.solid(toColor),
            iconTint = toContrastColor,
            textStyle = UI.typo.b2.style(
                color = toContrastColor,
                fontWeight = FontWeight.ExtraBold
            ),
            padding = 10.dp,
        ) {
            onSetExpanded()
        }
    }

    val transferMiniBottomSpacer = 20 * percentCollapsed
    if (transferMiniBottomSpacer > 0f) {
        Spacer(modifier = Modifier.height(transferMiniBottomSpacer.dp))
    }
}

@Composable
@Suppress("ParameterNaming")
private fun SheetHeader(
    percentExpanded: Float,
    onFullHeight: (Int) -> Unit,
    label: String,
    type: TransactionType,
    accounts: List<Account>,
    selectedAccount: Account?,
    toAccount: Account?,
    onSelectedAccountChanged: (Account) -> Unit,
    onToAccountChanged: (Account) -> Unit,
    onAddNewAccount: () -> Unit,
) {
    if (percentExpanded > 0.01f) {
        Column(
            modifier = Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    onFullHeight(placeable.height)

                    val height = placeable.height * percentExpanded

                    layout(placeable.width, height.roundToInt()) {
                        placeable.placeRelative(
                            x = 0,
                            y = -(height * (1f - percentExpanded)).roundToInt(),
                        )
                    }
                }
                .alpha(percentExpanded)
        ) {
            Spacer(Modifier.height(32.dp))

            Text(
                modifier = Modifier.padding(start = 32.dp),
                text = label,
                style = UI.typo.b1.style(
                    color = UI.colors.pureInverse,
                    fontWeight = FontWeight.ExtraBold
                )
            )

            Spacer(Modifier.height(if (type == TransactionType.TRANSFER) 8.dp else 16.dp))

            AccountsRow(
                accounts = accounts,
                selectedAccount = selectedAccount,
                onSelectedAccountChanged = onSelectedAccountChanged,
                onAddNewAccount = onAddNewAccount,
                childrenTestTag = "from_account"
            )

            if (type == TransactionType.TRANSFER) {
                Spacer(Modifier.height(24.dp))

                Text(
                    modifier = Modifier.padding(start = 32.dp),
                    text = stringResource(R.string.to),
                    style = UI.typo.b1.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.ExtraBold
                    )
                )

                Spacer(Modifier.height(8.dp))

                AccountsRow(
                    accounts = accounts,
                    selectedAccount = toAccount,
                    onSelectedAccountChanged = onToAccountChanged,
                    onAddNewAccount = onAddNewAccount,
                    childrenTestTag = "to_account",
                )
            }
        }
    }
}

@Composable
@Suppress("ParameterNaming")
private fun AccountsRow(
    accounts: List<Account>,
    selectedAccount: Account?,
    onSelectedAccountChanged: (Account) -> Unit,
    modifier: Modifier = Modifier,
    childrenTestTag: String? = null,
    onAddNewAccount: () -> Unit,
) {
    val lazyState = rememberLazyListState()

    LaunchedEffect(accounts, selectedAccount) {
        if (selectedAccount != null) {
            val selectedIndex = accounts.indexOf(selectedAccount)
            if (selectedIndex != -1) {
                launch {
                    if (TestingContext.inTest) return@launch // breaks UI tests

                    lazyState.scrollToItem(
                        index = selectedIndex, // +1 because Spacer width 24.dp
                    )
                }
            }
        }
    }

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        state = lazyState
    ) {
        item {
            Spacer(Modifier.width(24.dp))
        }

        itemsIndexed(accounts) { _, account ->
            Account(
                account = account,
                selected = selectedAccount == account,
                testTag = childrenTestTag ?: "account"
            ) {
                onSelectedAccountChanged(account)
            }
            Spacer(Modifier.width(8.dp))
        }

        item {
            AddAccount {
                onAddNewAccount()
            }
        }

        item {
            Spacer(Modifier.width(24.dp))
        }
    }
}

@Composable
private fun Account(
    account: Account,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val accountColor = account.color.toComposeColor()
    val textColor =
        if (selected) findContrastTextColor(accountColor) else UI.colors.pureInverse

    val medium = UI.colors.medium
    val rFull = UI.shapes.rFull

    Row(
        modifier = Modifier
            .clip(UI.shapes.rFull)
            .thenIf(!selected) {
                border(2.dp, medium, rFull)
            }
            .thenIf(selected) {
                background(accountColor, rFull)
            }
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        ItemIconSDefaultIcon(
            iconName = account.icon,
            defaultIcon = R.drawable.ic_custom_account_s,
            tint = textColor
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = account.name,
            style = UI.typo.b2.style(
                color = textColor,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun AddAccount(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(UI.shapes.rFull)
            .border(2.dp, UI.colors.medium, UI.shapes.rFull)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(12.dp))

        IvyIcon(
            icon = R.drawable.ic_plus,
            tint = UI.colors.pureInverse
        )

        Spacer(Modifier.width(4.dp))

        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = stringResource(R.string.add_account),
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Composable
private fun Amount(
    type: TransactionType,
    amount: Double,
    currency: String,
    percentExpanded: Float,
    label: String,
    account: Account?,
    onShowAmountModal: () -> Unit,
    showConvertedAmountText: String? = null,
    onAccountMiniClick: () -> Unit

) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val percentCollapsed = 1f - percentExpanded
        val balanceFontSize = lerp(40, 30, percentCollapsed)
        val currencyFontSize = lerp(30, 18, percentCollapsed)

        Spacer(Modifier.width(32.dp))

        if (percentExpanded > 0.01f) {
            Spacer(
                Modifier.weight(
                    (1f * percentExpanded).coerceAtLeast(0.01f)
                )
            )
        }

        Column {
            BalanceRow(
                modifier = Modifier
                    .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                        onShowAmountModal()
                    }
                    .testTag("edit_amount_balance_row"),
                currency = currency,
                balance = amount,

                spacerCurrency = 8.dp,

                balanceFontSize = balanceFontSize.sp,
                currencyFontSize = currencyFontSize.sp,

                currencyUpfront = false
            )
            if (showConvertedAmountText != null) {
                Text(
                    text = showConvertedAmountText,
                    style = UI.typo.nB2.style(
                        color = UI.colors.pureInverse,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Spacer(Modifier.weight(1f))

        if (percentExpanded < 1f && type != TransactionType.TRANSFER) {
            LabelAccountMini(
                percentExpanded = percentExpanded,
                label = label,
                account = account,
                onClick = onAccountMiniClick
            )
        }

        Spacer(Modifier.width(32.dp))
    }
}

@Composable
private fun LabelAccountMini(
    percentExpanded: Float,
    label: String,
    account: Account?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)

                val width = placeable.width * (1f - percentExpanded)

                layout(width.roundToInt(), placeable.height) {
                    placeable.placeRelative(
                        x = 0,
                        y = 0
                    )
                }
            }
            .alpha(1f - percentExpanded)
            .clickableNoIndication(
                interactionSource = rememberInteractionSource(),
                role = Role.Button,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = UI.typo.nC.style(
                color = UI.colors.mediumInverse,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = account?.name?.uppercase(Locale.getDefault()) ?: "",
            style = UI.typo.nB2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.ExtraBold
            )
        )
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = null,
                type = TransactionType.INCOME,
                ActionButton = {
                    ModalSet {
                    }
                },
                accounts = listOf(
                    acc1,
                    Account("DSK", color = GreenDark.toArgb()),
                    Account("phyre", color = GreenLight.toArgb()),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = null,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}

@Preview
@Composable
private fun Preview_Transfer() {
    IvyWalletPreview {
        val acc1 = Account("Cash", color = Green.toArgb())
        val acc2 = Account("DSK", color = GreenDark.toArgb())

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
        ) {
            EditBottomSheet(
                amountModalShown = false,
                setAmountModalShown = {},
                initialTransactionId = UUID.randomUUID(),
                ActionButton = {
                    ModalSave {
                    }
                },
                type = TransactionType.TRANSFER,
                accounts = listOf(
                    acc1,
                    acc2,
                    Account("phyre", color = GreenLight.toArgb(), icon = "cash"),
                    Account("Revolut", color = IvyDark.toArgb()),
                ),
                selectedAccount = acc1,
                toAccount = acc2,
                amount = 12350.0,
                currency = "BGN",
                onAmountChanged = {},
                onSelectedAccountChanged = {},
                onToAccountChanged = {},
                onAddNewAccount = {}
            )
        }
    }
}
