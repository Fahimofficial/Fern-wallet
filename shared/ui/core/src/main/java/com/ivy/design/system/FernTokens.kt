package com.ivy.design.system

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.ui.R

/**
 * Fern design tokens for Material 3 screens. New and migrated screens use these (through
 * `MaterialTheme` and [FernSpacing]) instead of the deprecated `UI.*` design system and raw
 * dp/sp literals. See docs/Design-System.md.
 */
object FernSpacing {
    val XSmall = 4.dp
    val Small = 8.dp
    val Medium = 12.dp
    val Large = 16.dp
    val XLarge = 24.dp
    val XXLarge = 32.dp

    /** Horizontal padding of screen content, matching the legacy screens. */
    val ScreenHorizontal = 16.dp

    /** Minimum size of anything tappable (Material accessibility guideline). */
    val MinTouchTarget = 48.dp
}

/** Same corner radii as the legacy design (r4 = 16, r2 = 24, r1 = 32). */
val FernShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/**
 * Raleway for headings and labels, Open Sans for body text and numbers: the app's two fonts.
 * The only definition of their weight mapping; the legacy design system uses it too.
 */
object FernFonts {
    val Raleway = FontFamily(
        Font(R.font.raleway_light, FontWeight.Light),
        Font(R.font.raleway_regular, FontWeight.Normal),
        Font(R.font.raleway_medium, FontWeight.Medium),
        Font(R.font.raleway_semibold, FontWeight.SemiBold),
        Font(R.font.raleway_bold, FontWeight.Bold),
        Font(R.font.raleway_extrabold, FontWeight.ExtraBold),
        Font(R.font.raleway_black, FontWeight.Black),
    )

    // Open Sans has no Medium or Black files: Medium uses Regular and Black uses Bold, as the
    // legacy design always did.
    val OpenSans = FontFamily(
        Font(R.font.opensans_regular, FontWeight.Normal),
        Font(R.font.opensans_regular, FontWeight.Medium),
        Font(R.font.opensans_semibold, FontWeight.SemiBold),
        Font(R.font.opensans_bold, FontWeight.Bold),
        Font(R.font.opensans_extrabold, FontWeight.ExtraBold),
        Font(R.font.opensans_bold, FontWeight.Black),
    )
}

private fun heading(size: Int, weight: FontWeight, lineHeight: Int): TextStyle = TextStyle(
    fontFamily = FernFonts.Raleway,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

private fun body(size: Int, weight: FontWeight, lineHeight: Int): TextStyle = TextStyle(
    fontFamily = FernFonts.OpenSans,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
)

/** Sizes follow the legacy scale (40/32/20/16/12 sp) mapped onto the Material 3 roles. */
@Suppress("MagicNumber")
val FernTypography = Typography(
    displayLarge = heading(40, FontWeight.Black, 48),
    displayMedium = heading(32, FontWeight.ExtraBold, 40),
    displaySmall = heading(28, FontWeight.ExtraBold, 36),
    headlineLarge = heading(32, FontWeight.ExtraBold, 40),
    headlineMedium = heading(28, FontWeight.ExtraBold, 36),
    headlineSmall = heading(24, FontWeight.ExtraBold, 32),
    titleLarge = heading(20, FontWeight.Bold, 28),
    titleMedium = heading(16, FontWeight.Bold, 24),
    titleSmall = heading(14, FontWeight.Bold, 20),
    bodyLarge = body(16, FontWeight.Normal, 24),
    bodyMedium = body(14, FontWeight.Normal, 20),
    bodySmall = body(12, FontWeight.Normal, 16),
    labelLarge = heading(14, FontWeight.Bold, 20),
    labelMedium = heading(12, FontWeight.ExtraBold, 16),
    labelSmall = heading(11, FontWeight.ExtraBold, 16),
)
