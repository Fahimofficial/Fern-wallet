package com.ivy.design.system

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernPalette
import com.ivy.design.system.appearance.LocalFernPalette
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.design.system.appearance.resolve
import com.ivy.design.system.colors.IvyColors

/**
 * Material3 theme built from the same [FernPalette] as the old design system, so both kinds of
 * screens share one set of colours. Also provides [LocalFernPalette].
 */
@Composable
fun IvyMaterial3Theme(
    isTrueBlack: Boolean,
    dark: Boolean = isSystemInDarkTheme(),
    palette: FernPalette = AppearanceSettings.Default.resolve(dark, isTrueBlack),
    content: @Composable () -> Unit
) {
    val colorScheme = remember(palette) { palette.toColorScheme() }
    CompositionLocalProvider(LocalFernPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

private fun FernPalette.toColorScheme(): ColorScheme {
    val background = this[ColorToken.Background]
    val text = this[ColorToken.TextPrimary]
    val accent = this[ColorToken.Accent]
    // Accents are tuned as fills; on dark backgrounds Material also uses primary as text and icons.
    val primary = if (isLight) accent else lerp(accent, Color.White, DarkPrimaryLift)
    val secondary = this[ColorToken.AccentSecondary]
    val error = this[ColorToken.Overdue]
    val elevated = this[ColorToken.Elevated]
    val card = this[ColorToken.Card]
    val outline = this[ColorToken.Outline]
    fun container(color: Color) = lerp(color, background, ContainerMix)
    return ColorScheme(
        primary = primary,
        onPrimary = primary.contentColor(),
        primaryContainer = container(accent),
        onPrimaryContainer = text,
        inversePrimary = accent,
        secondary = secondary,
        onSecondary = secondary.contentColor(),
        secondaryContainer = container(secondary),
        onSecondaryContainer = text,
        tertiary = secondary,
        onTertiary = secondary.contentColor(),
        tertiaryContainer = container(secondary),
        onTertiaryContainer = text,
        background = background,
        onBackground = text,
        surface = background,
        onSurface = text,
        surfaceVariant = card,
        onSurfaceVariant = text,
        surfaceTint = text,
        inverseSurface = text,
        inverseOnSurface = background,
        error = error,
        onError = error.contentColor(),
        errorContainer = container(error),
        onErrorContainer = text,
        outline = outline,
        outlineVariant = lerp(outline, background, OutlineVariantMix),
        scrim = (if (isLight) IvyColors.ExtraDarkGray else IvyColors.ExtraLightGray).copy(alpha = ScrimAlpha),
        surfaceBright = elevated,
        surfaceContainer = elevated,
        surfaceContainerHigh = elevated,
        surfaceContainerHighest = card,
        surfaceContainerLow = elevated,
        surfaceContainerLowest = background,
        surfaceDim = background,
    )
}

private const val DarkPrimaryLift = 0.4f
private const val ContainerMix = 0.7f
private const val OutlineVariantMix = 0.5f
private const val ScrimAlpha = 0.8f
