package com.ivy.navigation

import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras

/**
 * Holds one [ViewModelStore] per non-legacy back-stack entry ([NavEntry.id]).
 *
 * It is itself a ViewModel of the Activity, so screen ViewModels survive configuration changes and
 * are cleared together with the Activity. An entry's store is cleared as soon as the entry has left
 * the back stack and its screen has left the composition (after its exit transition).
 *
 * This replaces clearing the whole Activity ViewModelStore on navigation, which also destroyed the
 * ViewModels of the screen being opened and of the app root (blank screens, dead buttons).
 */
class ScreenViewModelStores : ViewModel() {
    private val stores = HashMap<Long, ViewModelStore>()

    fun storeFor(entryId: Long): ViewModelStore = synchronized(stores) {
        stores.getOrPut(entryId) { ViewModelStore() }
    }

    fun clear(entryId: Long) {
        synchronized(stores) { stores.remove(entryId) }?.clear()
    }

    fun hasStore(entryId: Long): Boolean = synchronized(stores) { entryId in stores }

    internal fun clearAll() {
        val all = synchronized(stores) {
            stores.values.toList().also { stores.clear() }
        }
        all.forEach { it.clear() }
    }

    override fun onCleared() {
        clearAll()
    }
}

/** ViewModel owner of one back-stack entry. Creates ViewModels with the Activity's (Hilt) factory. */
internal class EntryViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    private val parent: ViewModelStoreOwner,
) : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelProviderFactory
            ?: ViewModelProvider.NewInstanceFactory()

    override val defaultViewModelCreationExtras: CreationExtras
        get() {
            val parentExtras = (parent as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras
                ?: CreationExtras.Empty
            // Keep SavedStateHandles of this entry in this entry's store, not the Activity's.
            return MutableCreationExtras(parentExtras).apply {
                set(VIEW_MODEL_STORE_OWNER_KEY, this@EntryViewModelStoreOwner)
            }
        }
}
