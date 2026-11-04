package com.ivy.accounts

sealed interface AccountsEvent {
    data class OnReorder(val reorderedList: List<com.ivy.legacy.data.model.AccountData>) :
        AccountsEvent
    data class OnReorderModalVisible(val reorderVisible: Boolean) : AccountsEvent

    /** The tab is shown again after another tab: reload its data. */
    data object Refresh : AccountsEvent
}
