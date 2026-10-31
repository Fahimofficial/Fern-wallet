package com.ivy.wallet.domain.deprecated.logic

import com.ivy.base.legacy.Transaction
import com.ivy.base.time.TimeProvider
import com.ivy.data.db.dao.read.PlannedPaymentRuleDao
import com.ivy.data.repository.TransactionRepository
import com.ivy.data.repository.mapper.TransactionMapper
import com.ivy.legacy.datamodel.PlannedPaymentRule
import com.ivy.legacy.domain.deprecated.logic.PlannedOccurrences
import com.ivy.legacy.datamodel.temp.toDomain
import com.ivy.legacy.datamodel.temp.toLegacyDomain
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates the upcoming transactions of planned payments.
 *
 * Recurring rules are generated in a rolling window: up to [PlannedOccurrences.HORIZON_YEARS] years
 * from today (not from the rule's start date) and at most [PlannedOccurrences.UPCOMING_LIMIT]
 * upcoming instances. [extendAll] tops the window up (e.g. on app start), so rules keep producing
 * upcoming payments however long ago they started.
 */
@Singleton
class PlannedPaymentsGenerator @Inject constructor(
    private val transactionMapper: TransactionMapper,
    private val transactionRepository: TransactionRepository,
    private val plannedPaymentRuleDao: PlannedPaymentRuleDao,
    private val timeProvider: TimeProvider,
) {
    // Saving a rule (regenerate) and topping up (extend) must not interleave, or instances
    // could be created twice.
    private val mutex = Mutex()

    /** Replaces the rule's upcoming instances; already paid ones are kept. Call after saving a rule. */
    suspend fun generate(rule: PlannedPaymentRule): Unit = mutex.withLock {
        // delete all not happened transactions
        transactionRepository.deletedByRecurringRuleIdAndNoDateTime(
            recurringRuleId = rule.id
        )

        if (rule.oneTime) {
            generateOneTime(rule)
        } else {
            val paid = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id).size
            rule.schedule()?.let { schedule ->
                PlannedOccurrences.regenerate(schedule, paidCount = paid, now = timeProvider.utcNow())
                    .forEach { generateTransaction(rule, it) }
            }
        }
    }

    /** Adds upcoming instances to every recurring rule whose window has moved on. Never deletes. */
    suspend fun extendAll() {
        plannedPaymentRuleDao.findAllByOneTime(oneTime = false)
            .map { it.toLegacyDomain() }
            .forEach { extend(it) }
    }

    private suspend fun extend(rule: PlannedPaymentRule): Unit = mutex.withLock {
        val schedule = rule.schedule() ?: return@withLock
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id)
        val upcoming = existing.filter { !it.settled }.map { it.time }
        val dueDates = if (upcoming.isEmpty()) {
            // Everything was paid or skipped: continue after the paid ones, as regenerating would.
            PlannedOccurrences.regenerate(schedule, paidCount = existing.size, now = timeProvider.utcNow())
        } else {
            PlannedOccurrences.extend(schedule, upcomingDueDates = upcoming, now = timeProvider.utcNow())
        }
        dueDates.forEach { generateTransaction(rule, it) }
    }

    private fun PlannedPaymentRule.schedule(): PlannedOccurrences.Schedule? {
        val start = startDate
        val type = intervalType
        val n = intervalN?.takeIf { it > 0 }
        return if (start != null && type != null && n != null) {
            PlannedOccurrences.Schedule(start = start, intervalType = type, intervalN = n)
        } else {
            null
        }
    }

    private suspend fun generateOneTime(rule: PlannedPaymentRule) {
        val trns = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id)

        if (trns.isEmpty()) {
            generateTransaction(rule, rule.startDate!!)
        }
    }

    private suspend fun generateTransaction(rule: PlannedPaymentRule, dueDate: Instant) {
        Transaction(
            type = rule.type,
            accountId = rule.accountId,
            recurringRuleId = rule.id,
            categoryId = rule.categoryId,
            amount = rule.amount.toBigDecimal(),
            title = rule.title,
            description = rule.description,
            dueDate = dueDate,
            dateTime = null,
            toAccountId = null,
            isSynced = false
        ).toDomain(transactionMapper)?.let {
            transactionRepository.save(it)
        }
    }
}
