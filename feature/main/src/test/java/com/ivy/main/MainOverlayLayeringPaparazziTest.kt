package com.ivy.main

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.home.HomeSampleOverlay
import com.ivy.home.HomeSampleUi
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

/** Home's sheets must cover the bottom bar and the + button (they didn't inside AnimatedContent). */
@RunWith(TestParameterInjector::class)
class MainOverlayLayeringPaparazziTest(
    @param:TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot period picker above bottom bar`() {
        snapshot(theme) {
            IvyWalletPreview(if (theme == PaparazziTheme.Dark) Theme.DARK else Theme.LIGHT) {
                MainTabs(
                    tab = MainTab.HOME,
                    home = { HomeSampleUi(HomeSampleOverlay.PeriodPicker) },
                    accounts = {},
                )
                BottomBar(
                    tab = MainTab.HOME,
                    selectTab = {},
                    onAddIncome = {},
                    onAddExpense = {},
                    onAddTransfer = {},
                    onAddPlannedPayment = {},
                    showAddAccountModal = {},
                )
            }
        }
    }
}
