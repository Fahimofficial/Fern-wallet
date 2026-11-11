package com.ivy.home

import com.ivy.data.datasource.SupportCardsPrefs
import io.kotest.matchers.shouldBe
import org.junit.Test

class SupportCardsUiTest {
    // When the app was installed or updated: the second one is later, a different install.
    private val v1 = 1_700_000_000_000L
    private val v2 = 1_700_000_900_000L

    /** Defaults of a fresh install; [SupportCardsPrefs] itself has none. */
    private fun prefs(
        permanentSupportCard: Boolean = true,
        helpDismissedFor: Long? = null,
        supportDismissedFor: Long? = null,
    ) = SupportCardsPrefs(permanentSupportCard, helpDismissedFor, supportDismissedFor)

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
        supportCardsUi(prefs(helpDismissedFor = v1), v1) shouldBe
            SupportCardsUi(help = false, highlightedSupport = true, permanentSupport = false)

        supportCardsUi(prefs(supportDismissedFor = v1), v1) shouldBe
            SupportCardsUi(help = true, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `once both are dismissed the permanent card takes the place of the support card`() {
        supportCardsUi(prefs(helpDismissedFor = v1, supportDismissedFor = v1), v1) shouldBe
            SupportCardsUi(help = false, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `a new install or update brings both one-time cards back, even with the same version code`() {
        val dismissedForV1 = prefs(helpDismissedFor = v1, supportDismissedFor = v1)

        supportCardsUi(dismissedForV1, v2) shouldBe
            SupportCardsUi(help = true, highlightedSupport = true, permanentSupport = false)

        // Dismissing in the new version hides them again, whatever happened in the old one.
        supportCardsUi(prefs(helpDismissedFor = v2, supportDismissedFor = v2), v2) shouldBe
            SupportCardsUi(help = false, highlightedSupport = false, permanentSupport = true)
    }

    @Test
    fun `turning the permanent card off never hides the one-time support card`() {
        supportCardsUi(prefs(permanentSupportCard = false), v1).highlightedSupport shouldBe true

        // Dismissed and switched off: no Support My Work card at all, but the help card is unaffected.
        supportCardsUi(
            prefs(permanentSupportCard = false, supportDismissedFor = v1),
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
