package com.ivy.home

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.navigation.HelpSupportScreen
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.SupportScreen
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.performClickOnTag
import io.kotest.matchers.shouldBe
import org.junit.Test

class HomeSupportCardsTest : PaparazziRenderTest() {
    private val bothOneTimeCards = SupportCardsUi(help = true, highlightedSupport = true, permanentSupport = false)

    /** Renders the cards, taps [tag] once they are laid out, and reports whether it was tappable. */
    private fun tap(
        tag: String,
        navigation: Navigation = Navigation(),
        ui: SupportCardsUi = bothOneTimeCards,
        onDismissHelp: () -> Unit = {},
        onDismissSupport: () -> Unit = {},
    ): Boolean {
        var tapped = false
        render {
            IvyWalletPreview(Theme.LIGHT) {
                // Innermost, so the cards navigate with this Navigation.
                NavigationRoot(navigation) {
                    val view = LocalView.current
                    Box(Modifier.onGloballyPositioned { if (!tapped) tapped = view.performClickOnTag(tag) }) {
                        SupportCards(ui, onDismissHelp, onDismissSupport)
                    }
                }
            }
        }
        return tapped
    }

    @Test
    fun `the Help and Support card opens Help and Support`() {
        val navigation = Navigation()

        tap(HelpCardTag, navigation) shouldBe true

        navigation.currentScreen shouldBe HelpSupportScreen
    }

    @Test
    fun `the one-time Support My Work card opens the donation screen`() {
        val navigation = Navigation()

        tap(SupportCardTag, navigation) shouldBe true

        navigation.currentScreen shouldBe SupportScreen
    }

    @Test
    fun `the permanent Support My Work card opens the donation screen`() {
        val navigation = Navigation()

        tap(SupportCardTag, navigation, SupportCardsUi(false, false, permanentSupport = true)) shouldBe true

        navigation.currentScreen shouldBe SupportScreen
    }

    @Test
    fun `dismissing the help card does not dismiss the support card`() {
        var helpDismissed = false
        var supportDismissed = false

        tap(
            HelpCardDismissTag,
            onDismissHelp = { helpDismissed = true },
            onDismissSupport = { supportDismissed = true },
        )

        helpDismissed shouldBe true
        supportDismissed shouldBe false
    }

    @Test
    fun `dismissing the support card does not dismiss the help card`() {
        var helpDismissed = false
        var supportDismissed = false

        tap(
            SupportCardDismissTag,
            onDismissHelp = { helpDismissed = true },
            onDismissSupport = { supportDismissed = true },
        )

        supportDismissed shouldBe true
        helpDismissed shouldBe false
    }

    @Test
    fun `a hidden card cannot be tapped`() {
        tap(HelpCardTag, ui = SupportCardsUi.Hidden) shouldBe false
    }
}
