package com.ivy.settings.support

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

class HelpSupportNavigationTest : PaparazziRenderTest() {
    @Test
    fun `Support My Work opens the native donation screen and back returns to Help and Support`() {
        val navigation = Navigation().apply { navigateTo(HelpSupportScreen) }
        var clicked = false

        render {
            IvyWalletPreview(Theme.LIGHT) {
                // Innermost, so the screen navigates with this Navigation.
                NavigationRoot(navigation) {
                    val view = LocalView.current
                    Box(
                        Modifier.onGloballyPositioned {
                            if (!clicked) clicked = view.performClickOnTag(SupportMyWorkRowTag)
                        }
                    ) {
                        HelpSupportUi(onOpenUrl = {})
                    }
                }
            }
        }

        clicked shouldBe true
        navigation.currentScreen shouldBe SupportScreen
        navigation.onBackPressed()
        navigation.currentScreen shouldBe HelpSupportScreen
    }
}
