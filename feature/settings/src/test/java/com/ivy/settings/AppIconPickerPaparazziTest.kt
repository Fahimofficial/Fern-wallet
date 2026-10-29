package com.ivy.settings

import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.testing.PaparazziScreenshotTest
import com.ivy.ui.testing.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(TestParameterInjector::class)
class AppIconPickerPaparazziTest(
    @param:TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {
    @Test
    fun `snapshot app icon picker`() {
        snapshot(theme) {
            AppIconPickerUiTest(isDark = theme == PaparazziTheme.Dark)
        }
    }
}
