package com.ivy.home

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.navigation.Navigation
import com.ivy.navigation.NavigationRoot
import com.ivy.navigation.SupportScreen
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.performClickOnTag
import io.kotest.matchers.shouldBe
import org.junit.Test

class HomeSupportCardTest : PaparazziRenderTest() {
    @Test
    fun `tapping Support my work in the Home menu opens the Support screen`() {
        val navigation = Navigation()
        var clicked = false

        render {
            IvyWalletPreview(Theme.LIGHT) {
                // Innermost, so the menu navigates with this Navigation.
                NavigationRoot(navigation) {
                    val view = LocalView.current
                    BoxWithConstraints(
                        Modifier
                            .fillMaxSize()
                            .onGloballyPositioned { if (!clicked) clicked = view.performClickOnTag(SupportCardTag) }
                    ) {
                        HomeSampleUi(overlay = HomeSampleOverlay.MoreMenu)
                    }
                }
            }
        }

        clicked shouldBe true
        navigation.currentScreen shouldBe SupportScreen
    }
}
