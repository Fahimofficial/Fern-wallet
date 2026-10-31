package com.ivy.wallet.domain.deprecated.logic

import android.database.SQLException
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
import timber.log.Timber
import java.time.DateTimeException
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

    /**
     * Adds upcoming instances to every recurring rule whose window has moved on. Never deletes.
     * A rule that can't be extended (invalid dates, database error) is logged and skipped, so it
     * doesn't stop the others. Other exceptions are bugs and propagate.
     */
    suspend fun extendAll() {
        val rules = try {
            plannedPaymentRuleDao.findAllByOneTime(oneTime = false)
        } catch (e: SQLException) {
            Timber.e(e, "Couldn't load planned payment rules")
            return
        }
        for (rule in rules.map { it.toLegacyDomain() }) {
            try {
                extend(rule)
            } catch (e: SQLException) {
                logExtendFailure(rule, e)
            } catch (e: DateTimeException) {
                logExtendFailure(rule, e)
            } catch (e: ArithmeticException) {
                // Date arithmetic overflow for an absurd start date or interval.
                logExtendFailure(rule, e)
            }
        }
    }

    private fun logExtendFailure(rule: PlannedPaymentRule, e: Exception) {
        Timber.e(e, "Couldn't extend planned payment rule ${rule.id}")
    }

    private suspend fun extend(rule: PlannedPaymentRule): Unit = mutex.withLock {
        val schedule = rule.schedule() ?: return@withLock
        val existing = transactionRepository.findAllByRecurringRuleId(recurringRuleId = rule.id)
        val upcoming = existing.filter { !it.settled }.map { it.time }
        PlannedOccurrences.extend(
            schedule = schedule,
            upcomingDueDates = upcoming,
            paidCount = existing.size - upcoming.size,
            now = timeProvider.utcNow(),
        ).forEach { generateTransaction(rule, it) }
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
