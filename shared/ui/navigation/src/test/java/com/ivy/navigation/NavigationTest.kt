package com.ivy.navigation

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.Test
import java.util.UUID

class NavigationTest {

    private val cardA = CreditCardDetailsScreen(UUID(0, 1))
    private val editA = EditCreditCardScreen(UUID(0, 1))

    @Test
    fun `navigating forward creates newer entries and back restores the previous entry`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val main = nav.currentEntry!!
        nav.navigateTo(cardA)
        val details = nav.currentEntry!!

        details.id shouldNotBe main.id
        (details.id > main.id).shouldBeTrue()
        nav.currentScreen shouldBe cardA

        nav.back().shouldBeTrue()
        nav.currentEntry shouldBeSameInstanceAs main
        nav.back().shouldBeFalse()
    }

    @Test
    fun `entries stay alive while on the back stack and die when popped`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(cardA)
        val details = nav.currentEntry!!
        nav.navigateTo(editA)
        val edit = nav.currentEntry!!

        // Card details are under the edit screen: their ViewModels must survive.
        nav.isAlive(details).shouldBeTrue()
        nav.isAlive(edit).shouldBeTrue()

        nav.back()
        nav.isAlive(edit).shouldBeFalse()
        nav.isAlive(details).shouldBeTrue()

        nav.back()
        nav.isAlive(details).shouldBeFalse()
    }

    @Test
    fun `visiting the same screen twice gives two independent entries`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(cardA)
        val first = nav.currentEntry!!
        nav.navigateTo(editA)
        nav.navigateTo(cardA)
        val second = nav.currentEntry!!

        (second === first).shouldBeFalse()
        nav.back()
        nav.back()
        nav.currentEntry shouldBeSameInstanceAs first
    }

    @Test
    fun `navigating to the screen already shown keeps the same entry`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val main = nav.currentEntry!!

        nav.navigateTo(MainScreen)

        nav.currentEntry shouldBeSameInstanceAs main
        // Like before entries existed, back is then a visual no-op that stays on the same entry.
        nav.back().shouldBeTrue()
        nav.currentEntry shouldBeSameInstanceAs main
    }

    @Test
    fun `reset back stack releases every previous entry except the shown one`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        val main = nav.currentEntry!!
        nav.navigateTo(cardA)
        val details = nav.currentEntry!!

        nav.resetBackStack()
        nav.isAlive(main).shouldBeFalse()
        nav.isAlive(details).shouldBeTrue() // still on screen

        nav.navigateTo(MainScreen)
        nav.isAlive(details).shouldBeFalse()
        nav.backStackEmpty().shouldBeTrue()
    }

    @Test
    fun `back is left to the system only when there is nothing to go back to`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.handlesBack().shouldBeFalse()
        nav.predictiveBackTarget() shouldBe null

        nav.navigateTo(cardA)
        nav.handlesBack().shouldBeTrue()
        nav.predictiveBackTarget()!!.screen shouldBe MainScreen
    }

    @Test
    @Suppress("DEPRECATION")
    fun `an open modal or a screen override disables the predictive preview but keeps back in the app`() {
        val nav = Navigation()
        nav.navigateTo(MainScreen)
        nav.navigateTo(cardA)

        nav.modalBackHandling.add(Navigation.ModalBackHandler(UUID(0, 9)) { true })
        nav.handlesBack().shouldBeTrue()
        nav.hasOpenModal().shouldBeTrue()
        nav.predictiveBackTarget() shouldBe null
        nav.modalBackHandling.pop()
        nav.hasOpenModal().shouldBeFalse()

        nav.onBackPressed[cardA] = { false }
        nav.predictiveBackTarget() shouldBe null
        nav.onBackPressed.remove(cardA)
        nav.predictiveBackTarget()!!.screen shouldBe MainScreen
    }

    @Test
    fun `before-back hooks run when leaving their screen by back only`() {
        val nav = Navigation()
        var calls = 0
        nav.navigateTo(MainScreen)
        nav.navigateTo(cardA)
        nav.beforeBack[cardA] = { calls++ }

        nav.navigateTo(editA)
        calls shouldBe 0
        nav.back()
        calls shouldBe 0
        nav.back()
        calls shouldBe 1
        nav.currentScreen shouldBe MainScreen
    }
}
