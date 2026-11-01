package com.ivy.data

import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.model.Category
import com.ivy.data.model.CategoryId
import com.ivy.data.model.Tag
import com.ivy.data.model.TagId
import com.ivy.data.model.sync.UniqueId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataObserver @Inject constructor() {
    private val _writeEvents = MutableSharedFlow<DataWriteEvent>()
    val writeEvents: Flow<DataWriteEvent> = _writeEvents

    /**
     * Incremented on every [DataWriteEvent.AllDataChange] (backup import, undo, data wipe).
     * In-memory caches compare it to drop data written behind their back.
     */
    @Volatile
    var allDataChangeGeneration: Int = 0
        private set

    suspend fun post(event: DataWriteEvent) {
        if (event == DataWriteEvent.AllDataChange) {
            synchronized(this) { allDataChangeGeneration++ }
        }
        _writeEvents.emit(event)
    }
}

sealed interface DataWriteEvent {
    data object AllDataChange : AccountChange, CategoryChange

    sealed interface AccountChange : DataWriteEvent
    data class SaveAccounts(val accounts: List<Account>) : AccountChange
    data class DeleteAccounts(val operation: DeleteOperation<AccountId>) : AccountChange

    /** Card details or statement overrides changed (affects how card accounts are shown). */
    data class CreditCardsChanged(val accountIds: List<AccountId>) : AccountChange

    sealed interface CategoryChange : DataWriteEvent
    data class SaveCategories(val categories: List<Category>) : CategoryChange
    data class DeleteCategories(val operation: DeleteOperation<CategoryId>) : CategoryChange

    /**
     * Transactions were written outside the screen that shows them (e.g. a deleted transaction
     * restored by Undo after its edit screen closed). Lists and balances should reload.
     */
    data object TransactionsChanged : DataWriteEvent

    sealed interface TagChange : DataWriteEvent
    data class SaveTags(val tags: List<Tag>) : TagChange
    data class DeleteTags(val operation: DeleteOperation<TagId>) : TagChange
}

sealed interface DeleteOperation<out Id : UniqueId> {
    data object All : DeleteOperation<Nothing>
    data class Just<Id : UniqueId>(val ids: List<Id>) : DeleteOperation<Id>
}
