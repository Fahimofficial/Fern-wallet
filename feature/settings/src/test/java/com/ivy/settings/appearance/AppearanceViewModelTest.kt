package com.ivy.settings.appearance

import com.ivy.domain.features.BoolFeature
import com.ivy.domain.features.Features
import com.ivy.domain.features.IvyFeatures
import io.mockk.every
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ivy.base.legacy.Theme
import com.ivy.base.legacy.appContext
import com.ivy.data.datasource.AppearanceDataSource
import com.ivy.design.system.appearance.AppearancePreset
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.decodeAppearance
import com.ivy.design.system.appearance.withColor
import com.ivy.design.system.appearance.withPreset
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.datamodel.Settings
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.wallet.domain.action.settings.SettingsAct
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppearanceViewModelTest {
    private val ivyContext = run {
        // IvyWalletCtx reads month names through the legacy global context.
        @Suppress("DEPRECATION")
        appContext = mockk<Context>(relaxed = true)
        IvyWalletCtx()
    }
    private var stored: String? = null
    private val dataSource = mockk<AppearanceDataSource> {
        coEvery { set(any()) } answers { stored = firstArg() }
        coEvery { get() } answers { stored }
    }
    private val settings = mockk<Settings>(relaxed = true)
    private val settingsAct = mockk<SettingsAct> { coEvery { getSettings() } returns settings }
    private val updateSettingsAct = mockk<UpdateSettingsAct>(relaxed = true)
    private val accountColorsFlag = mockk<BoolFeature>(relaxed = true)
    private val features = mockk<Features> { every { showAccountColorsInTransactions } returns accountColorsFlag }
    private val mainDispatcher = UnconfinedTestDispatcher()
    private lateinit var applicationScope: CoroutineScope
    private lateinit var viewModel: AppearanceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(mainDispatcher)
        applicationScope = CoroutineScope(mainDispatcher + SupervisorJob())
        viewModel = AppearanceViewModel(
            ivyContext,
            dataSource,
            settingsAct,
            updateSettingsAct,
            applicationScope,
            features,
            mockk(relaxed = true),
        )
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `a change applies immediately and survives a restart`() {
        viewModel.update { it.withPreset(AppearancePreset.Ivy).withColor(dark = true, ColorToken.Income, Color.Cyan) }

        ivyContext.appearance.preset shouldBe AppearancePreset.Ivy
        // What the next app start reads back (RootViewModel decodes the same value).
        decodeAppearance(stored) shouldBe ivyContext.appearance
    }

    @Test
    fun `leaving the screen before a save starts doesn't lose the queued change`() = runTest(mainDispatcher) {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        applicationScope.cancel()
        applicationScope = CoroutineScope(dispatcher + SupervisorJob())
        val store = ViewModelStore()
        val screenViewModel = ViewModelProvider(
            store,
            viewModelFactory {
                initializer {
                    AppearanceViewModel(
                        ivyContext,
                        dataSource,
                        settingsAct,
                        updateSettingsAct,
                        applicationScope,
                        features,
                        mockk(relaxed = true),
                    )
                }
            },
        )[AppearanceViewModel::class.java]

        screenViewModel.update { it.withPreset(AppearancePreset.Midnight) }
        store.clear() // the screen-scoped ViewModel is gone: its scope is cancelled
        testScheduler.advanceUntilIdle()

        decodeAppearance(stored).preset shouldBe AppearancePreset.Midnight
    }

    @Test
    fun `reset to default is saved too`() {
        viewModel.update { it.withColor(dark = false, ColorToken.Card, Color.Gray) }
        viewModel.update { AppearanceSettings.Default }

        decodeAppearance(stored) shouldBe AppearanceSettings.Default
    }

    @Test
    fun `theme mode is applied and persisted, re-selecting it does nothing`() {
        ivyContext.switchTheme(Theme.LIGHT)

        viewModel.setTheme(Theme.AMOLED_DARK)
        viewModel.setTheme(Theme.AMOLED_DARK)

        ivyContext.theme shouldBe Theme.AMOLED_DARK
        coVerify(exactly = 1) { updateSettingsAct(any()) }
    }

    @Test
    fun `the account colours switch writes the shared feature flag`() {
        viewModel.setAccountColorsInTransactions(true)
        coVerify { accountColorsFlag.set(any(), true) }
    }

    @Test
    fun `account colours on transactions are off by default`() {
        IvyFeatures().showAccountColorsInTransactions.defaultValue shouldBe false
    }
}
