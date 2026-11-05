package com.ivy.design.l0_system

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.ivy.base.legacy.Theme
import com.ivy.design.api.IvyDesign
import androidx.compose.ui.graphics.lerp
import com.ivy.design.system.IvyMaterial3Theme
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.resolve

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyColors = compositionLocalOf<IvyColors> { error("No IvyColors") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyTypography = compositionLocalOf<IvyTypography> { error("No IvyTypography") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
val LocalIvyShapes = compositionLocalOf<IvyShapes> { error("No IvyShapes") }

@Deprecated("Old design system. Use `:ivy-design` and Material3")
object UI {
    val colors: IvyColors
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyColors.current

    val typo: IvyTypography
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyTypography.current

    val shapes: IvyShapes
        @Composable
        @ReadOnlyComposable
        get() = LocalIvyShapes.current
}

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyTheme(
    theme: Theme,
    design: IvyDesign,
    appearance: AppearanceSettings = AppearanceSettings.Default,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isTrueBlack = theme == Theme.AMOLED_DARK
    val dark = when (theme) {
        Theme.LIGHT -> false
        Theme.DARK, Theme.AMOLED_DARK -> true
        Theme.AUTO -> isDarkTheme
    }
    val palette = remember(appearance, dark, isTrueBlack) { appearance.resolve(dark, isTrueBlack) }
    // `design` is a new object on every call site recomposition and colors()/typography()/shapes()
    // build new objects each time. Providing a new instance through the CompositionLocals would
    // recompose every UI.colors/UI.typo/UI.shapes reader in the screen, so build them once per theme.
    val colors = remember(theme, isDarkTheme, palette) { design.colors(theme, isDarkTheme).withPalette(palette) }
    val typography = remember { design.typography() }
    val shapes = remember { design.shapes() }

    CompositionLocalProvider(
        LocalIvyColors provides colors,
        LocalIvyTypography provides typography,
        LocalIvyShapes provides shapes
    ) {
        val view = LocalView.current
        if (!view.isInEditMode && view.context is Activity) {
            SideEffect {
                val window = (view.context as Activity).window
                window.statusBarColor = Color.Transparent.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = colors.isLight
                    // Follow the app theme, not the system one (e.g. app light, system dark).
                    isAppearanceLightNavigationBars = colors.isLight
                }
            }
        }

        IvyMaterial3Theme(
            dark = !colors.isLight,
            isTrueBlack = isTrueBlack,
            palette = palette,
            content = content,
        )
    }
}

/** The old colour slots, filled from the user's palette. The "1" variants are tints of their colour. */
private fun IvyColors.withPalette(palette: FernPalette): IvyColors {
    val base = this
    val background = palette[ColorToken.Background]
    val text = palette[ColorToken.TextPrimary]
    fun tint(token: ColorToken) = lerp(palette[token], background, TintMix)
    return object : IvyColors {
        override val pure = background
        override val pureInverse = text
        override val gray = palette[ColorToken.TextSecondary]
        override val medium = palette[ColorToken.Card]
        override val mediumInverse = base.mediumInverse
        override val primary = palette[ColorToken.Accent]
        override val primary1 = lerp(palette[ColorToken.Accent], text, if (palette.isLight) 0.4f else 0.75f)
        override val green = palette[ColorToken.Income]
        override val green1 = tint(ColorToken.Income)
        override val orange = palette[ColorToken.Upcoming]
        override val orange1 = tint(ColorToken.Upcoming)
        override val red = palette[ColorToken.Overdue]
        override val red1 = tint(ColorToken.Overdue)
        override val red1Inverse = lerp(palette[ColorToken.Overdue], text, TintMix)
        override val isLight = palette.isLight
    }
}

private const val TintMix = 0.55f
