package com.ivy.uitests

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.legacy.Theme
import com.ivy.home.HomeSampleUi
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.data.model.MainTab
import com.ivy.main.MainTabs
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Quick-access chevron on Home, inside the real tab container (where Home's root sits at
 * zIndex 1 and used to swallow the chevron's taps).
 */
@RunWith(AndroidJUnit4::class)
class HubChevronTest {
    @get:Rule
    val rule = createComposeRule()

    private var tab by mutableStateOf(MainTab.HOME)

    private fun setContent() {
        rule.setContent {
            IvyWalletPreview(Theme.LIGHT) {
                BoxWithConstraints(Modifier.fillMaxSize()) {
                    MainTabs(
                        tab = tab,
                        home = { HomeSampleUi(modifier = it, tabSelected = tab == MainTab.HOME) },
                        accounts = {},
                    )
                }
            }
        }
    }

    private val chevron get() = rule.onNodeWithTag(ChevronTag)
    private val hubContent get() = rule.onNodeWithText(QuickAccessTitle)

    @Test
    fun tapOpensAndClosesTheHub() {
        setContent()
        hubContent.assertDoesNotExist()

        chevron.performClick()
        rule.waitForIdle()
        hubContent.assertIsDisplayed()

        chevron.performClick()
        rule.waitForIdle()
        hubContent.assertDoesNotExist()
    }

    @Test
    fun dragOpensAndClosesTheHub() {
        setContent()

        chevron.performTouchInput { swipeDown() }
        rule.waitForIdle()
        hubContent.assertIsDisplayed()

        chevron.performTouchInput { swipeUp() }
        rule.waitForIdle()
        hubContent.assertDoesNotExist()
    }

    @Test
    fun chevronIsHiddenAndInertOnAccounts() {
        setContent()
        chevron.assertIsDisplayed()

        tab = MainTab.ACCOUNTS
        rule.waitForIdle()

        chevron.assertIsNotDisplayed()
        hubContent.assertDoesNotExist()
    }

    private companion object {
        const val ChevronTag = "home_more_menu_arrow"
        const val QuickAccessTitle = "Quick access"
    }
}
