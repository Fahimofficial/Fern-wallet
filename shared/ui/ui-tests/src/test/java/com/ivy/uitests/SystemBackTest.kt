package com.ivy.uitests

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.base.model.TransactionType
import com.ivy.navigation.EditTransactionScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.Screen
import com.ivy.navigation.SettingsScreen
import com.ivy.navigation.TransactionsScreen
import com.ivy.wallet.ui.theme.modal.AddModalBackHandling
import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * System back (gesture or button, both reach the activity's dispatcher) from the transaction
 * editor and from an account. Each destination first opens two legacy modals and closes them out
 * of order, the sequence that used to leave a handler behind and make back do nothing.
 */
@RunWith(AndroidJUnit4::class)
class SystemBackTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val navigation = Navigation()
    private var keypadOpen by mutableStateOf(false)
    private var pickerOpen by mutableStateOf(false)

    @Test
    fun backLeavesTheTransactionEditor() {
        assertBackReturnsToList(
            EditTransactionScreen(initialTransactionId = UUID(1, 1), type = TransactionType.EXPENSE)
        )
    }

    @Test
    fun backLeavesTheAccountScreen() {
        assertBackReturnsToList(TransactionsScreen(accountId = UUID(2, 2)))
    }

    @Test
    fun backClosesAnOpenModalBeforeTheScreen() {
        setContent()
        open(TransactionsScreen(accountId = UUID(3, 3)))
        pickerOpen = true
        rule.waitForIdle()

        pressBack()
        pickerOpen shouldBe false
        navigation.currentScreen shouldBe TransactionsScreen(accountId = UUID(3, 3))

        pressBack()
        navigation.currentScreen shouldBe ListScreen
    }

    private fun assertBackReturnsToList(destination: Screen) {
        setContent()
        open(destination)

        // Amount keypad opens, the category picker opens over it, the keypad closes first.
        keypadOpen = true
        rule.waitForIdle()
        pickerOpen = true
        rule.waitForIdle()
        keypadOpen = false
        rule.waitForIdle()
        pickerOpen = false
        rule.waitForIdle()

        pressBack()
        navigation.currentScreen shouldBe ListScreen
        rule.onNodeWithText(ListText).assertExists()
    }

    private fun setContent() {
        navigation.navigateTo(ListScreen)
        rule.setContent {
            NavigationRoot(navigation) { screen ->
                if (screen == ListScreen) {
                    Text(ListText)
                } else {
                    Text("destination")
                    val keypadId = remember { UUID.randomUUID() }
                    val pickerId = remember { UUID.randomUUID() }
                    AddModalBackHandling(modalId = keypadId, visible = keypadOpen) { keypadOpen = false }
                    AddModalBackHandling(modalId = pickerId, visible = pickerOpen) { pickerOpen = false }
                }
            }
        }
        rule.waitForIdle()
    }

    private fun open(screen: Screen) {
        rule.runOnIdle { navigation.navigateTo(screen) }
        rule.waitForIdle()
    }

    private fun pressBack() {
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
    }

    private companion object {
        val ListScreen: Screen = SettingsScreen
        const val ListText = "list"
    }
}
