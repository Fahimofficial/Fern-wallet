package com.ivy.settings.appearance

import com.ivy.design.system.appearance.ColorGroup
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.readableOn
import com.ivy.design.system.motion.IvyMotion
import com.ivy.ui.R

/**
 * A miniature of the app drawn only from [palette], so each colour change is visible in context
 * before leaving the screen. Purely decorative for screen readers: the rows below describe it all.
 */
@Composable
internal fun AppearancePreview(palette: FernPalette, modifier: Modifier = Modifier) {
    // Changes sweep through the miniature as a wave: balance and text, then money, then bars.
    val c: @Composable (ColorToken) -> Color = { token ->
        val delay = when (token.group) {
            ColorGroup.Money -> WaveStepMillis
            ColorGroup.Progress, ColorGroup.Navigation -> 2 * WaveStepMillis
            else -> 0
        }
        animateColorAsState(
            palette[token],
            IvyMotion.standard(delayMillis = delay),
            label = "preview_${token.name}",
        ).value
    }
    val background = c(ColorToken.Background)
    val card = c(ColorToken.Card)
    val text = c(ColorToken.TextPrimary)
    val secondary = c(ColorToken.TextSecondary)
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            .background(background)
            .border(1.dp, c(ColorToken.Outline), shape)
            .clearAndSetSemantics { }
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.appearance_preview_balance),
            style = UI.typo.c.style(color = secondary, fontWeight = FontWeight.Bold)
        )
        Text(text = "12,480.00", style = UI.typo.nH2.style(color = text, fontWeight = FontWeight.Black))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryCard(R.drawable.ic_income, c(ColorToken.Income), "+3,200.00", card)
            SummaryCard(R.drawable.ic_expense, c(ColorToken.Expense), "-1,845.20", card)
        }
        Spacer(Modifier.height(8.dp))
        TransactionRow(R.string.appearance_preview_salary, c(ColorToken.Income), "+3,200.00", card, text)
        TransactionRow(R.string.appearance_preview_groceries, c(ColorToken.Expense), "-42.50", card, text)
        TransactionRow(R.string.appearance_preview_rent, c(ColorToken.Upcoming), "-950.00", card, text, warning = true)
        Spacer(Modifier.height(10.dp))
        ProgressRow(card, c(ColorToken.BudgetProgress), c(ColorToken.Overdue), c(ColorToken.Transfer))
        Spacer(Modifier.height(12.dp))
        NavBarRow(NavTokens.associateWith { c(it) })
    }
}

@Composable
private fun RowScope.SummaryCard(@DrawableRes icon: Int, color: Color, amount: String, card: Color) {
    Row(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(icon, color)
        Spacer(Modifier.width(8.dp))
        Text(text = amount, style = UI.typo.nC.style(color = color.readableOn(card), fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun TransactionRow(
    label: Int,
    color: Color,
    amount: String,
    card: Color,
    text: Color,
    // "Due soon" isn't told by the amber alone: it gets a warning mark too.
    warning: Boolean = false,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(card)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(label),
            style = UI.typo.c.style(color = text, fontWeight = FontWeight.Bold)
        )
        if (warning) {
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.ic_overdue),
                contentDescription = null,
                tint = color.readableOn(card),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(text = amount, style = UI.typo.nC.style(color = color.readableOn(card), fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun ProgressRow(card: Color, budget: Color, overdue: Color, transfer: Color) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(card)
    ) {
        Box(Modifier.weight(BudgetShare).height(8.dp).background(budget))
        Box(Modifier.weight(TransferShare).height(8.dp).background(transfer))
        Box(Modifier.weight(OverShare).height(8.dp).background(overdue))
        Spacer(Modifier.weight(1f - BudgetShare - TransferShare - OverShare))
    }
}

@Composable
private fun NavBarRow(colors: Map<ColorToken, Color>) {
    val fab = colors.getValue(ColorToken.Fab)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.getValue(ColorToken.NavBar))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_home),
            contentDescription = null,
            tint = colors.getValue(ColorToken.NavSelected),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(fab),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add),
                contentDescription = null,
                tint = fab.contentColor(),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.ic_accounts),
            contentDescription = null,
            tint = colors.getValue(ColorToken.NavUnselected),
        )
        Spacer(Modifier.width(12.dp))
        // Accent button on a dialog surface.
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(colors.getValue(ColorToken.Elevated))
                .padding(4.dp)
        ) {
            Box(
                Modifier
                    .size(width = 28.dp, height = 16.dp)
                    .clip(CircleShape)
                    .background(colors.getValue(ColorToken.Accent))
            )
        }
    }
}

@Composable
private fun Badge(@DrawableRes icon: Int, color: Color) {
    Box(
        Modifier.size(24.dp).clip(CircleShape).background(color),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color.contentColor(),
            modifier = Modifier.size(18.dp),
        )
    }
}

private val NavTokens = listOf(
    ColorToken.NavBar,
    ColorToken.NavSelected,
    ColorToken.NavUnselected,
    ColorToken.Fab,
    ColorToken.Accent,
    ColorToken.Elevated,
)
private const val WaveStepMillis = 60
private const val BudgetShare = 0.45f
private const val TransferShare = 0.15f
private const val OverShare = 0.1f
