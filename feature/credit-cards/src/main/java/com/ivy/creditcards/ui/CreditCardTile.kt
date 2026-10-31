package com.ivy.creditcards.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.creditcard.StatementStatus
import com.ivy.ui.R
import java.time.LocalDate

/** Card row in the Accounts list: who issued it, what's owed, what's left, when it's due. */
@Composable
fun CreditCardTile(
    tile: CreditCardTileUi,
    today: LocalDate,
    hideAmounts: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = tile.visual.appearance.palette()
    val closed = tile.status == CardStatus.CLOSED
    // The tile carries the card's own colours: a soft wash of its design behind the content and a
    // tinted outline, strong enough to tell cards apart, light enough to keep text readable in
    // light, dark and AMOLED themes. Closed cards are toned down.
    // Deep design colour on light surfaces, its light accent on dark/AMOLED surfaces.
    val identity = if (UI.colors.isLight) palette.end else palette.accent
    val tint = identity.copy(alpha = if (closed) TintAlphaClosed else TintAlpha)
    val wash = remember(tint) {
        Brush.horizontalGradient(listOf(tint, tint.copy(alpha = tint.alpha * WashEndFraction)))
    }
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(wash)
            .border(1.dp, identity.copy(alpha = if (closed) BorderAlphaClosed else BorderAlpha), UI.shapes.r4)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(16.dp)
            .testTag("credit_card_tile")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Mini card in the card's design
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 34.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .cardSurface(palette),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram(tile.visual.issuerName ?: tile.visual.nickname),
                    style = UI.typo.c.style(color = Color.White, fontWeight = FontWeight.ExtraBold)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = tile.visual.nickname,
                    style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        tile.visual.issuerName,
                        tile.visual.lastFour?.let { "•• $it" },
                        tile.visual.network?.displayName,
                    ).joinToString(" · "),
                    style = UI.typo.nC.style(color = UI.colors.gray),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = stringResource(
                        if (tile.outstanding.signum() < 0) R.string.cc_credit_balance else R.string.cc_outstanding
                    ),
                    style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
                )
                Text(
                    text = if (hideAmounts) HIDDEN else tile.outstanding.abs().money(tile.currency),
                    style = UI.typo.nB1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold)
                )
            }
        }

        if (tile.creditLimit != null && tile.availableCredit != null && !hideAmounts) {
            Spacer(Modifier.height(12.dp))
            UtilisationBar(utilisation = tile.utilisation ?: 0f)
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.cc_available_of_limit,
                    tile.availableCredit.money(tile.currency),
                    tile.creditLimit.money(tile.currency),
                ),
                style = UI.typo.nC.style(color = UI.colors.gray)
            )
        }

        val due = tile.due
        if (tile.status != CardStatus.CLOSED && (due != null || tile.statementClosing != null)) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tile.statementClosing?.let {
                    Text(
                        text = stringResource(R.string.cc_statement_on, it.shortDate(today)),
                        style = UI.typo.nC.style(color = UI.colors.gray)
                    )
                } ?: Spacer(Modifier.width(1.dp))
                if (due != null) DueChip(due = due, currency = tile.currency, today = today, hideAmounts = hideAmounts)
            }
        }
    }
}

@Composable
fun UtilisationBar(utilisation: Float, modifier: Modifier = Modifier) {
    val color = when {
        utilisation >= HighUtilisation -> DueTone.DANGER.color(UI.colors)
        utilisation >= MediumUtilisation -> DueTone.WARNING.color(UI.colors)
        else -> DueTone.GOOD.color(UI.colors)
    }
    val percent = (utilisation * PERCENT).toInt()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(UI.colors.medium)
            .semantics { stateDescription = "$percent% of limit used" }
    ) {
        Box(
            Modifier
                .fillMaxWidth(utilisation.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
    }
}

@Composable
fun DueChip(
    due: DueSummary,
    currency: String,
    today: LocalDate,
    hideAmounts: Boolean,
    modifier: Modifier = Modifier,
) {
    val tone = due.tone
    val text = when (due.status) {
        StatementStatus.PAID -> stringResource(R.string.cc_status_paid)
        StatementStatus.NOTHING_DUE -> stringResource(R.string.cc_status_nothing_due)
        StatementStatus.OVERDUE -> stringResource(
            R.string.cc_overdue_amount,
            if (hideAmounts) HIDDEN else due.remaining.money(currency)
        )
        else -> {
            val amount = if (hideAmounts) HIDDEN else due.remaining.money(currency)
            due.dueDate?.let {
                if (it == today) {
                    stringResource(R.string.cc_due_today_amount, amount)
                } else {
                    stringResource(R.string.cc_due_on_amount, it.shortDate(today), amount)
                }
            } ?: stringResource(R.string.cc_due_amount, amount)
        }
    }
    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(tone.color(UI.colors).copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = text,
        style = UI.typo.c.style(color = tone.color(UI.colors), fontWeight = FontWeight.ExtraBold)
    )
}

private const val HIDDEN = "••••"
private const val TintAlpha = 0.12f
private const val TintAlphaClosed = 0.06f
private const val WashEndFraction = 0.35f
private const val BorderAlpha = 0.45f
private const val BorderAlphaClosed = 0.2f
private const val HighUtilisation = 0.9f
private const val MediumUtilisation = 0.5f
private const val PERCENT = 100
