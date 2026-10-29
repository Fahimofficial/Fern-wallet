package com.ivy.wallet.domain.action.account

import arrow.core.nonEmptyListOf
import com.ivy.base.time.TimeConverter
import com.ivy.base.time.TimeProvider
import com.ivy.data.model.Account
import com.ivy.data.model.AccountType
import com.ivy.data.repository.CreditCardRepository
import com.ivy.frp.action.FPAction
import com.ivy.frp.then
import com.ivy.legacy.domain.pure.transaction.AccountValueFunctions
import com.ivy.wallet.domain.pure.data.ClosedTimeRange
import com.ivy.wallet.domain.pure.transaction.foldTransactions
import java.math.BigDecimal
import javax.inject.Inject

class CalcAccBalanceAct @Inject constructor(
    private val accTrnsAct: AccTrnsAct,
    private val timeProvider: TimeProvider,
    private val creditCardRepository: CreditCardRepository,
    private val timeConverter: TimeConverter,
) : FPAction<CalcAccBalanceAct.Input, CalcAccBalanceAct.Output>() {

    override suspend fun Input.compose(): suspend () -> Output = suspend {
        AccTrnsAct.Input(
            accountId = account.id.value,
            range = range ?: ClosedTimeRange.allTimeIvy(timeProvider)
        )
    } then accTrnsAct then { accTrns ->
        foldTransactions(
            transactions = accTrns,
            arg = account.id.value,
            valueFunctions = nonEmptyListOf(AccountValueFunctions::balance)
        ).head
    } then { balance ->
        Output(
            account = account,
            balance = balance - creditCardOpeningOutstanding(
                account = account,
                range = range ?: ClosedTimeRange.allTimeIvy(timeProvider),
            )
        )
    }

    /**
     * A credit card's outstanding when it was added to Fern Wallet. It is part of the card's
     * balance (as a liability) but intentionally NOT a transaction, so it never shows up as
     * income or expense in any report.
     */
    private suspend fun creditCardOpeningOutstanding(
        account: Account,
        range: ClosedTimeRange,
    ): BigDecimal {
        val card = account.takeIf { it.type == AccountType.CREDIT_CARD }
            ?.let { creditCardRepository.findByAccountId(it.id) }
            ?: return BigDecimal.ZERO
        val openingInstant = with(timeConverter) { card.openingDate.atStartOfDay().toUTC() }
        val inRange = !openingInstant.isBefore(range.from) && !openingInstant.isAfter(range.to)
        return if (inRange) card.openingOutstanding else BigDecimal.ZERO
    }

    @Suppress("DataClassDefaultValues")
    data class Input(
        val account: Account,
        val range: ClosedTimeRange? = null
    )

    data class Output(
        val account: Account,
        val balance: BigDecimal,
    )
}
