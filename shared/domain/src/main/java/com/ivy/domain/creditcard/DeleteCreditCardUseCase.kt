package com.ivy.domain.creditcard

import com.ivy.data.db.dao.write.WritePlannedPaymentRuleDao
import com.ivy.data.model.AccountId
import com.ivy.data.model.Expense
import com.ivy.data.model.Income
import com.ivy.data.model.Transaction
import com.ivy.data.model.Transfer
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CreditCardRepository
import com.ivy.data.repository.TransactionRepository
import javax.inject.Inject

/**
 * Deletes a credit card, its own transactions and its card details, without changing the
 * balance or history of any other account.
 *
 * Transfers between the card and another account (bill payments, money moved off the card)
 * would otherwise be orphaned: the transaction mapper rejects a transfer whose other account
 * no longer exists, so the bank would silently lose those payments and its balance would jump.
 * Each completed transfer is therefore rewritten as the other account's side of it, keeping the
 * same id, date, amount, tags and notes:
 * - payment into the card -> expense on the paying account (the money still left it)
 * - transfer out of the card -> income on the receiving account
 *
 * Planned (unpaid) transfers involving the card are removed, since they can't be completed anymore.
 */
class DeleteCreditCardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val creditCardRepository: CreditCardRepository,
    private val plannedPaymentRuleWriter: WritePlannedPaymentRuleDao,
) {
    /**
     * @param fallbackTitle title for a preserved payment that had none (e.g. "Payment to <card>").
     */
    suspend fun delete(cardId: AccountId, fallbackTitle: String?) {
        val incoming = transactionRepository.findAllTransfersToAccount(cardId)
            .filter { it.fromAccount != cardId }
        val outgoing = transactionRepository.findAllTransferByAccount(cardId)
            .filter { it.toAccount != cardId }
        val title = fallbackTitle?.let { NotBlankTrimmedString.from(it).getOrNull() }

        val preserved = preservedSides(incoming, outgoing, title)
        if (preserved.isNotEmpty()) transactionRepository.saveMany(preserved)
        // Unpaid incoming transfers still point at the card (outgoing ones go with deleteAllByAccountId).
        incoming.filterNot { it.settled }.forEach { transactionRepository.deleteById(it.id) }

        transactionRepository.deleteAllByAccountId(cardId)
        plannedPaymentRuleWriter.deletedByAccountId(cardId.value)
        creditCardRepository.deleteByAccountId(cardId)
        accountRepository.deleteById(cardId)
    }

    companion object {
        /** The other account's side of each completed transfer with the card. */
        fun preservedSides(
            incoming: List<Transfer>,
            outgoing: List<Transfer>,
            fallbackTitle: NotBlankTrimmedString?,
        ): List<Transaction> =
            incoming.filter { it.settled }.map { trn ->
                Expense(
                    id = trn.id,
                    title = trn.title ?: fallbackTitle,
                    description = trn.description,
                    category = trn.category,
                    time = trn.time,
                    settled = true,
                    metadata = trn.metadata,
                    tags = trn.tags,
                    value = trn.fromValue,
                    account = trn.fromAccount,
                )
            } + outgoing.filter { it.settled }.map { trn ->
                Income(
                    id = trn.id,
                    title = trn.title ?: fallbackTitle,
                    description = trn.description,
                    category = trn.category,
                    time = trn.time,
                    settled = true,
                    metadata = trn.metadata,
                    tags = trn.tags,
                    value = trn.toValue,
                    account = trn.toAccount,
                )
            }
    }
}
