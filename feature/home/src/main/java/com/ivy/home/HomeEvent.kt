package com.ivy.home

import com.ivy.base.legacy.Transaction
import com.ivy.home.customerjourney.CustomerJourneyCardModel
import com.ivy.legacy.data.model.TimePeriod

sealed interface HomeEvent {
    data class SetUpcomingExpanded(val expanded: Boolean) : HomeEvent
    data class SetOverdueExpanded(val expanded: Boolean) : HomeEvent

    data object BalanceClick : HomeEvent
    data object HiddenBalanceClick : HomeEvent
    data object HiddenIncomeClick : HomeEvent
    data class SetExpanded(val expanded: Boolean) : HomeEvent

    data object SwitchTheme : HomeEvent

    data class SetBuffer(val buffer: Double) : HomeEvent

    data class SetCurrency(val currency: String) : HomeEvent

    data class SetPeriod(val period: TimePeriod) : HomeEvent

    data class PayOrGetPlanned(val transaction: Transaction) : HomeEvent
    data class SkipPlanned(val transaction: Transaction) : HomeEvent
    data class SkipAllPlanned(val transactions: List<Transaction>) : HomeEvent

    data class DismissCustomerJourneyCard(val card: CustomerJourneyCardModel) : HomeEvent

    data object SelectNextMonth : HomeEvent
    data object SelectPreviousMonth : HomeEvent

    /** Swiped away on Home: deleted at once, with Undo. */
    data class DeleteTransaction(val transaction: Transaction) : HomeEvent

    /** The eye next to the balance: hide or show it, remembered like the Settings switch. */
    data object ToggleHideBalance : HomeEvent

    /** The savings-goal celebration played: don't play it again for this goal amount. */
    data object SavingsGoalCelebrated : HomeEvent

    /** The one-time Help & Support card was dismissed: hidden until the next app version. */
    data object DismissHelpCard : HomeEvent

    /** The one-time Support My Work card was dismissed: hidden until the next app version. */
    data object DismissSupportCard : HomeEvent

    /** The tab is shown again after another tab: reload its data. */
    data object Refresh : HomeEvent
}
