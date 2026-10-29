package com.ivy.settings

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class SupportSectionPaparazziTest(
    @param:TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot support section`() {
        snapshot(theme) {
            SupportSectionUiTest(isDark = theme == PaparazziTheme.Dark)
        }
    }
}
