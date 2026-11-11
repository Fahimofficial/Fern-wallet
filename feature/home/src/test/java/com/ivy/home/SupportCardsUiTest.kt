package com.ivy.home

import com.ivy.data.datasource.SupportCardsPrefs
import io.kotest.matchers.shouldBe
import org.junit.Test

class SupportCardsUiTest {
    private val v1 = 206
    private val v2 = 207

    /** Defaults of a fresh install; [SupportCardsPrefs] itself has none. */
    private fun prefs(
        permanentSupportCard: Boolean = true,
        helpDismissedIn: Int? = null,
        supportDismissedIn: Int? = null,
    ) = SupportCardsPrefs(permanentSupportCard, helpDismissedIn, supportDismissedIn)

    @Test
    fun `a fresh install shows both one-time cards and not the permanent one`() {
        supportCardsUi(prefs(), v1) shouldBe
            SupportCardsUi(help = true, highlightedSupport = true, permanentSupport = false)
    }

    @Test
    fun `nothing is shown until the stored values are read`() {
        supportCardsUi(null, v1) shouldBe SupportCardsUi.Hidden
    }

    @Test
    fun `dismissing the help card leaves the support card, and the other way round`() {
        supportCardsUi(prefs(helpDismissedIn = v1), v1) shouldBe
            SupportCardsUi(help = false, highlightedSupport = true, permanentSupport = false)

        supportCardsUi(prefs(supportDismissedIn = v1), v1) shouldBe
            SupportCardsUi(help = true, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `once both are dismissed the permanent card takes the place of the support card`() {
        supportCardsUi(prefs(helpDismissedIn = v1, supportDismissedIn = v1), v1) shouldBe
            SupportCardsUi(help = false, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `an update brings both one-time cards back, each dismissed per version`() {
        val dismissedInV1 = prefs(helpDismissedIn = v1, supportDismissedIn = v1)

        supportCardsUi(dismissedInV1, v2) shouldBe
            SupportCardsUi(help = true, highlightedSupport = true, permanentSupport = false)

        // Dismissing in the new version hides them again, whatever happened in the old one.
        supportCardsUi(prefs(helpDismissedIn = v2, supportDismissedIn = v2), v2) shouldBe
            SupportCardsUi(help = false, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `turning the permanent card off never hides the one-time support card`() {
        supportCardsUi(prefs(permanentSupportCard = false), v1).highlightedSupport shouldBe true

        // Dismissed and switched off: no Support My Work card at all, but the help card is unaffected.
        supportCardsUi(
            prefs(permanentSupportCard = false, supportDismissedIn = v1),
            v1,
        ) shouldBe SupportCardsUi(help = true, highlightedSupport = false, permanentSupport = false)
    }

    @Test
    fun `two Support My Work cards are never shown together`() {
        val versions = listOf(null, v1, v2)
        for (enabled in listOf(true, false)) {
            for (helpIn in versions) {
                for (supportIn in versions) {
                    val ui = supportCardsUi(prefs(enabled, helpIn, supportIn), v1)
                    (ui.highlightedSupport && ui.permanentSupport) shouldBe false
                }
            }
        }
    }
}
