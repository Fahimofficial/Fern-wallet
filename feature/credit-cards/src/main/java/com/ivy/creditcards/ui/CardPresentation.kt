package com.ivy.creditcards.ui

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.ivy.data.creditcard.CardCatalogue
import com.ivy.data.model.creditcard.CardStatus
import com.ivy.data.model.creditcard.CreditCard
import com.ivy.design.l0_system.IvyColors
import com.ivy.domain.creditcard.CreditCardWithOverview
import com.ivy.domain.creditcard.StatementStatus
import com.ivy.legacy.utils.format
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Orange
import com.ivy.wallet.ui.theme.Red
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject

/** Formats an amount for display only (calculations stay in BigDecimal). */
fun BigDecimal.money(currency: String): String = "${toDouble().format(currency)} $currency"

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val MEDIUM_DATE: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun LocalDate.shortDate(today: LocalDate): String =
    if (year == today.year) format(SHORT_DATE) else format(MEDIUM_DATE)

fun LocalDate.mediumDate(): String = format(MEDIUM_DATE)

/** What the user should notice about the current bill. */
enum class DueTone { NEUTRAL, GOOD, WARNING, DANGER }

@Immutable
data class DueSummary(
    val status: StatementStatus,
    val dueDate: LocalDate?,
    val remaining: BigDecimal,
    val daysLeft: Long?,
) {
    val tone: DueTone
        get() = when (status) {
            StatementStatus.OVERDUE -> DueTone.DANGER
            StatementStatus.DUE_TODAY -> DueTone.WARNING
            StatementStatus.UPCOMING, StatementStatus.PARTIALLY_PAID ->
                if (daysLeft != null && daysLeft <= SOON_DAYS) DueTone.WARNING else DueTone.NEUTRAL
            StatementStatus.PAID, StatementStatus.NOTHING_DUE -> DueTone.GOOD
            StatementStatus.CARRIED_FORWARD -> DueTone.NEUTRAL
        }

    companion object {
        const val SOON_DAYS = 5L
    }
}

/** Plain function (not @Composable) so callers stay skippable; pass `UI.colors`. */
@Suppress("MagicNumber") // colour literals tuned for contrast on light backgrounds
fun DueTone.color(colors: IvyColors): Color = when (this) {
    DueTone.NEUTRAL -> colors.pureInverse
    DueTone.GOOD -> if (colors.isLight) Color(0xFF077D59) else Green
    DueTone.WARNING -> if (colors.isLight) Color(0xFF9A5B00) else Orange
    DueTone.DANGER -> if (colors.isLight) Color(0xFFC62828) else Red
}

/** String resource for a statement status; use with `stringResource`. */
@StringRes
fun StatementStatus.labelRes(): Int =
    when (this) {
        StatementStatus.NOTHING_DUE -> R.string.cc_status_nothing_due
        StatementStatus.UPCOMING -> R.string.cc_status_upcoming
        StatementStatus.DUE_TODAY -> R.string.cc_status_due_today
        StatementStatus.PARTIALLY_PAID -> R.string.cc_status_partially_paid
        StatementStatus.PAID -> R.string.cc_status_paid
        StatementStatus.OVERDUE -> R.string.cc_status_overdue
        StatementStatus.CARRIED_FORWARD -> R.string.cc_status_carried_forward
    }

/** Compact data for a card row in the Accounts list. */
@Immutable
@Suppress("DataClassTypedIDs") // raw UUID keeps the UI model stable for Compose
data class CreditCardTileUi(
    val accountId: UUID,
    val visual: CardVisualData,
    val currency: String,
    val outstanding: BigDecimal,
    val creditLimit: BigDecimal?,
    val availableCredit: BigDecimal?,
    /** 0..1 clamped, for the progress bar. */
    val utilisation: Float?,
    val statementClosing: LocalDate?,
    val due: DueSummary?,
    val status: CardStatus,
    val orderNum: Double,
)

class CardUiMapper @Inject constructor(
    private val catalogue: CardCatalogue,
) {
    fun issuerName(card: CreditCard): String? =
        catalogue.bank(card.bankId)?.displayName ?: card.customBankName

    fun visual(nickname: String, card: CreditCard): CardVisualData = CardVisualData(
        nickname = nickname,
        issuerName = issuerName(card),
        productName = card.productName ?: catalogue.product(card.productId)?.name,
        network = card.network,
        lastFour = card.lastFour,
        appearance = card.appearance,
        closed = card.status == CardStatus.CLOSED,
    )

    fun tile(item: CreditCardWithOverview, today: LocalDate): CreditCardTileUi {
        val overview = item.overview
        val statement = overview.urgentStatement
        return CreditCardTileUi(
            accountId = item.account.id.value,
            visual = visual(item.account.name.value, item.card),
            currency = item.account.asset.code,
            outstanding = overview.outstanding,
            creditLimit = overview.creditLimit,
            availableCredit = overview.availableCredit,
            utilisation = overview.utilisation?.toFloat()?.coerceIn(0f, 1f),
            statementClosing = overview.currentCycle?.closingDate,
            due = statement?.let {
                DueSummary(
                    status = it.status,
                    dueDate = it.dueDate,
                    remaining = it.remainingDue,
                    daysLeft = it.dueDate?.let { due -> ChronoUnit.DAYS.between(today, due) },
                )
            },
            status = item.card.status,
            orderNum = item.account.orderNum,
        )
    }
}
