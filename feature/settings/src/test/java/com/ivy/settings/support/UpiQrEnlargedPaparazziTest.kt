package com.ivy.settings.support

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.base.legacy.Theme
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.testing.PaparazziRenderTest
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import com.ivy.ui.testing.performClickOnTag
import io.kotest.matchers.shouldBe
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class UpiQrEnlargedPaparazziTest(
    @param:TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot enlarged qr`() {
        snapshot(theme) {
            UpiQrEnlargedUiTest(isDark = theme == PaparazziTheme.Dark)
        }
    }
}

class UpiQrClickTest : PaparazziRenderTest() {
    @Test
    fun `the small QR code is a button that can be tapped`() {
        var clickable = false

        render {
            IvyWalletPreview(Theme.LIGHT) {
                val view = LocalView.current
                Box(Modifier.onGloballyPositioned { if (!clickable) clickable = view.performClickOnTag(UpiQrTag) }) {
                    UpiCard(
                        SupportScreenActions(
                            onBack = {},
                            onOpenUrl = {},
                            onCopyUpiId = {},
                            onOpenUpiApp = {},
                            onShareQr = {},
                            onSaveQr = {},
                        )
                    )
                }
            }
        }

        clickable shouldBe true
    }
}
