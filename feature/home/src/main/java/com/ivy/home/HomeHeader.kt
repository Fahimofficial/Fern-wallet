package com.ivy.home

import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.get
import androidx.annotation.DrawableRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.base.model.TransactionType
import com.ivy.design.api.LocalTimeConverter
import com.ivy.design.api.LocalTimeFormatter
import com.ivy.design.api.LocalTimeProvider
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.data.model.TimePeriod
import com.ivy.legacy.ivyWalletCtx
import com.ivy.legacy.ui.component.transaction.TransactionsDividerLine
import com.ivy.legacy.utils.clickableNoIndication
import com.ivy.legacy.utils.format
import com.ivy.legacy.utils.horizontalSwipeListener
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.rememberInteractionSource
import com.ivy.legacy.utils.rememberSwipeListenerState
import com.ivy.legacy.utils.springBounce
import com.ivy.legacy.utils.verticalSwipeListener
import com.ivy.navigation.PieChartStatisticScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.findContrastTextColor
import com.ivy.wallet.ui.theme.incomeTextColor
import com.ivy.wallet.ui.theme.components.BalanceRowMini
import com.ivy.wallet.ui.theme.components.IvyIcon
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.sp
import com.ivy.design.system.motion.AnimatedAmountText
import com.ivy.legacy.utils.shortenAmount
import com.ivy.legacy.utils.shouldShortAmount
import kotlin.math.absoluteValue
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.design.system.motion.pressScale
import androidx.compose.ui.semantics.Role

// Larger surfaces shrink less, so a pressed card reads as pressed, not as moving.
private const val CardPressedScale = 0.98f

@ExperimentalAnimationApi
@Composable
internal fun HomeHeader(
    expanded: Boolean,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
) {
    Column {
        // Read only in graphics layers and derived visibility checks: collapsing the header
        // doesn't recompose it on every animation frame.
        val percentExpanded = animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = springBounce(
                stiffness = Spring.StiffnessLow
            ),
            label = "Home Header Expand Collapse"
        )

        Spacer(Modifier.height(20.dp))

        HeaderStickyRow(
            percentExpanded = { percentExpanded.value },
            name = name,
            period = period,
            currency = currency,
            balance = balance,
            hideBalance = hideBalance,

            onShowMonthModal = onShowMonthModal,
            onBalanceClick = onBalanceClick,
            onHiddenBalanceClick = onHiddenBalanceClick,
            onSelectNextMonth = onSelectNextMonth,
            onSelectPreviousMonth = onSelectPreviousMonth,
        )

        Spacer(Modifier.height(16.dp))

        val showDivider by remember { derivedStateOf { percentExpanded.value < 0.5f } }
        if (showDivider) {
            TransactionsDividerLine(
                modifier = Modifier.graphicsLayer { alpha = 1f - percentExpanded.value },
                paddingHorizontal = 0.dp
            )
        }
    }
}

@Composable
private fun HeaderStickyRow(
    percentExpanded: () -> Float,
    name: String,
    period: TimePeriod,
    currency: String,
    balance: Double,
    onShowMonthModal: () -> Unit,
    onBalanceClick: () -> Unit,
    onSelectNextMonth: () -> Unit,
    hideBalance: Boolean,
    onHiddenBalanceClick: () -> Unit,
    onSelectPreviousMonth: () -> Unit,
) {
    val view = LocalView.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .graphicsLayer { alpha = percentExpanded() }
                    .testTag("home_greeting_text"),
                text = if (name.isNotNullOrBlank()) {
                    stringResource(
                        R.string.hi_name,
                        name,
                    )
                } else {
                    stringResource(R.string.hi)
                },
                style = UI.typo.b1.style(
                    fontWeight = FontWeight.ExtraBold,
                    color = UI.colors.pureInverse,
                ),
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
            )

            // Balance mini row
            val showMiniBalance by remember { derivedStateOf { percentExpanded() < 1f } }
            if (showMiniBalance) {
                BalanceRowMini(
                    modifier = Modifier
                        .graphicsLayer { alpha = 1f - percentExpanded() }
                        .clickableNoIndication(rememberInteractionSource(), role = Role.Button) {
                            if (hideBalance) {
                                onHiddenBalanceClick()
                            } else {
                                onBalanceClick()
                            }
                        },
                    currency = currency,
                    balance = balance,
                    shortenBigNumbers = true,
                    hiddenMode = hideBalance,
                    doubleRowDisplay = true,
                )
            }
        }

        MonthSelector(
            modifier = Modifier.horizontalSwipeListener(
                sensitivity = 75,
                state = rememberSwipeListenerState(),
                onSwipeLeft = {
                    IvyHaptics.tick(view)
                    onSelectNextMonth()
                },
                onSwipeRight = {
                    IvyHaptics.tick(view)
                    onSelectPreviousMonth()
                },
            ),
            text = period.toDisplayShort(
                startDateOfMonth = ivyWalletCtx().startDayOfMonth,
                timeConverter = LocalTimeConverter.current,
                timeProvider = LocalTimeProvider.current,
                timeFormatter = LocalTimeFormatter.current,
            ),
            onClick = onShowMonthModal,
        )

        Spacer(Modifier.width(12.dp))

        Spacer(Modifier.width(40.dp)) // settings menu button spacer
    }
}

/**
 * One pill that opens the month picker: calendar, month, and its own caret (the round button next
 * to it opens Quick access, not a month menu). A horizontal swipe still steps the month.
 */
@Composable
private fun MonthSelector(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(R.string.select_month)
    Row(
        modifier = modifier
            .pressScale(interactionSource)
            .clip(UI.shapes.rFull)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .defaultMinSize(minWidth = 130.dp, minHeight = 48.dp)
            .border(2.dp, UI.colors.medium, UI.shapes.rFull)
            .padding(start = 12.dp, end = 8.dp)
            .testTag("home_month_selector"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        IvyIcon(icon = R.drawable.ic_calendar, tint = UI.colors.pureInverse)
        Spacer(Modifier.width(4.dp))
        Text(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
            text = text,
            style = UI.typo.b2.style(fontWeight = FontWeight.Bold, color = UI.colors.pureInverse),
        )
        IvyIcon(icon = R.drawable.ic_expand_more, tint = UI.colors.gray)
    }
}

@ExperimentalAnimationApi
@Composable
fun CashFlowInfo(
    currency: String,
    balance: Double,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideBalance: Boolean,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
    onOpenMoreMenu: () -> Unit,
    onBalanceClick: () -> Unit,
    onHiddenBalanceClick: () -> Unit,
    modifier: Modifier = Modifier,
    onCashflowClick: () -> Unit = {},
    onToggleHideBalance: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .verticalSwipeListener(
                sensitivity = Constants.SWIPE_DOWN_THRESHOLD_OPEN_MORE_MENU,
                state = rememberSwipeListenerState(),
                onSwipeDown = {
                    onOpenMoreMenu()
                },
            ),
    ) {
        HomeBalance(
            currency = currency,
            balance = balance,
            hidden = hideBalance,
            onHiddenClick = onHiddenBalanceClick,
            onOpenDetails = onBalanceClick,
            onToggleHidden = onToggleHideBalance,
        )

        Spacer(modifier = Modifier.height(24.dp))

        IncomeExpenses(
            currency = currency,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            hideIncome = hideIncome,
            onHiddenIncomeClick = onHiddenIncomeClick
        )

        val cashflow = monthlyIncome - monthlyExpenses
        if (cashflow != 0.0 && !hideBalance) {
            Spacer(Modifier.height(12.dp))

            CashflowChip(
                text = stringResource(
                    R.string.cashflow,
                    (if (cashflow > 0) "+" else ""),
                    cashflow.format(currency),
                    currency,
                ),
                color = if (cashflow < 0) Gray else incomeTextColor(),
                onClick = onCashflowClick,
            )

            Spacer(Modifier.height(4.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * The big balance. It counts to each new value; a tap switches between compact ("114.64k") and
 * exact, a long press (or the TalkBack action) opens the balance details, and the eye hides it.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeBalance(
    currency: String,
    balance: Double,
    hidden: Boolean,
    onHiddenClick: () -> Unit,
    onOpenDetails: () -> Unit,
    onToggleHidden: () -> Unit,
) {
    var compact by rememberSaveable { mutableStateOf(true) }
    val short = compact && shouldShortAmount(balance)
    // Compose memoizes this lambda: it only changes with currency and short.
    val format = { value: Double -> "$currency ${if (short) shortenAmount(value) else value.format(currency)}" }
    val detailsLabel = stringResource(R.string.balance_details)
    val toggleLabel = stringResource(if (compact) R.string.show_exact_amount else R.string.show_compact_amount)
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    interactionSource = rememberInteractionSource(),
                    indication = null,
                    role = Role.Button,
                    onClickLabel = if (hidden) null else toggleLabel,
                    onLongClickLabel = detailsLabel,
                    onLongClick = onOpenDetails,
                    onClick = {
                        if (hidden) onHiddenClick() else compact = !compact
                    },
                )
                .semantics {
                    customActions = listOf(
                        CustomAccessibilityAction(detailsLabel) {
                            onOpenDetails()
                            true
                        }
                    )
                }
                .testTag("home_balance"),
        ) {
            val style = UI.typo.nH1.style(fontWeight = FontWeight.ExtraBold, color = UI.colors.pureInverse)
            if (hidden) {
                Text(text = "$currency ****", style = style)
            } else {
                // Exact amounts of 7+ digits don't fit at 40sp on a phone: step down instead of wrapping.
                val fitted = if (format(balance).length > ExactBalanceMaxChars) style.copy(fontSize = 30.sp) else style
                AnimatedAmountText(amount = balance, format = format, style = fitted)
            }
        }
        IconButton(onClick = onToggleHidden) {
            IvyIcon(
                icon = if (hidden) R.drawable.ic_hidden else R.drawable.ic_visible,
                tint = UI.colors.gray,
                contentDescription = stringResource(if (hidden) R.string.show_balance else R.string.hide_balance),
            )
        }
    }
}

private const val ExactBalanceMaxChars = 13

@Composable
private fun CashflowChip(
    text: String,
    color: Color,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .padding(start = 16.dp)
            .pressScale(interactionSource)
            .clip(UI.shapes.rFull)
            .background(color.copy(alpha = ChipTintAlpha), UI.shapes.rFull)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                role = Role.Button,
                onClickLabel = stringResource(R.string.reports),
                onClick = onClick,
            )
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 12.dp)
            .testTag("home_cashflow_chip"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = UI.typo.nB2.style(color = color, fontWeight = FontWeight.Bold))
        Spacer(Modifier.width(4.dp))
        IvyIcon(icon = R.drawable.ic_arrow_right, tint = color)
    }
}

private const val ChipTintAlpha = 0.12f

@Composable
private fun IncomeExpenses(
    currency: String,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    hideIncome: Boolean,
    onHiddenIncomeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))

        val nav = navigation()

        HeaderCard(
            icon = R.drawable.ic_income,
            badgeColor = FernTheme.palette[ColorToken.Income],
            label = stringResource(R.string.income),
            currency = currency,
            amount = monthlyIncome,
            testTag = "home_card_income"
        ) {
            if (hideIncome) {
                onHiddenIncomeClick()
            } else {
                nav.navigateTo(
                    PieChartStatisticScreen(
                        type = TransactionType.INCOME,
                    ),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        HeaderCard(
            icon = R.drawable.ic_expense,
            badgeColor = FernTheme.palette[ColorToken.Expense],
            label = stringResource(R.string.expenses),
            currency = currency,
            amount = monthlyExpenses.absoluteValue,
            testTag = "home_card_expense",
        ) {
            nav.navigateTo(
                PieChartStatisticScreen(
                    type = TransactionType.EXPENSE,
                ),
            )
        }

        Spacer(Modifier.width(16.dp))
    }
}

/**
 * Income / expense summary card.
 *
 * Neutral surface + a small semantic badge instead of a saturated gradient fill with a
 * colored glow: keeps the amount (the important part) at maximum contrast in every theme
 * and stops the two cards from competing with the primary action for attention.
 */
@Composable
private fun RowScope.HeaderCard(
    @DrawableRes icon: Int,
    badgeColor: Color,
    label: String,
    currency: String,
    amount: Double,
    testTag: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .weight(1f)
            .pressScale(interactionSource, pressedScale = CardPressedScale)
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IvyIcon(
                modifier = Modifier
                    .size(32.dp)
                    .background(badgeColor, CircleShape),
                icon = icon,
                tint = findContrastTextColor(badgeColor),
            )

            Spacer(Modifier.width(8.dp))

            Text(
                text = label,
                style = UI.typo.c.style(
                    color = UI.colors.gray,
                    fontWeight = FontWeight.ExtraBold,
                ),
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            val short = shouldShortAmount(amount)
            AnimatedAmountText(
                modifier = Modifier.alignByBaseline(),
                amount = amount,
                format = { value: Double -> if (short) shortenAmount(value) else value.format(currency) },
                style = UI.typo.nB1.style(fontWeight = FontWeight.Bold, color = UI.colors.pureInverse),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                modifier = Modifier.alignByBaseline(),
                text = currency,
                style = UI.typo.nB1.style(fontWeight = FontWeight.Normal, color = UI.colors.pureInverse),
            )
        }
    }
}
