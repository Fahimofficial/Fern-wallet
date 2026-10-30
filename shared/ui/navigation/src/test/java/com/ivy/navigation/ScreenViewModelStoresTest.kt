package com.ivy.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenViewModelStoresTest {

    class ScreenViewModel : ViewModel() {
        var cleared = false
        override fun onCleared() {
            cleared = true
        }
    }

    private val activityOwner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    @Before
    fun setUp() {
        // viewModelScope uses Dispatchers.Main.immediate
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun ScreenViewModelStores.viewModelOf(entryId: Long): ScreenViewModel =
        ViewModelProvider(
            EntryViewModelStoreOwner(storeFor(entryId), activityOwner)
        )[ScreenViewModel::class.java]

    @Test
    fun `each entry gets its own ViewModel and keeps it until cleared`() {
        val stores = ScreenViewModelStores()

        val first = stores.viewModelOf(1)
        val second = stores.viewModelOf(2)

        (first === second).shouldBeFalse()
        stores.viewModelOf(1) shouldBeSameInstanceAs first
    }

    @Test
    fun `clearing one entry leaves the others and the Activity's ViewModels alive`() {
        val stores = ScreenViewModelStores()
        val activityViewModel = ViewModelProvider(activityOwner)[ScreenViewModel::class.java]
        val leaving = stores.viewModelOf(1)
        val opened = stores.viewModelOf(2)

        stores.clear(1)

        leaving.cleared.shouldBeTrue()
        opened.cleared.shouldBeFalse()
        // The scope of the screen being opened keeps working (it used to be cancelled, which left
        // card screens stuck on a blank loading state and made their buttons do nothing).
        opened.viewModelScope.isActive.shouldBeTrue()
        activityViewModel.cleared.shouldBeFalse()
        stores.hasStore(1).shouldBeFalse()
        stores.hasStore(2).shouldBeTrue()
    }

    @Test
    fun `clearing the holder clears every entry`() {
        val stores = ScreenViewModelStores()
        val a = stores.viewModelOf(1)
        val b = stores.viewModelOf(2)

        stores.clearAll()

        a.cleared shouldBe true
        b.cleared shouldBe true
        stores.hasStore(1).shouldBeFalse()
    }
}
