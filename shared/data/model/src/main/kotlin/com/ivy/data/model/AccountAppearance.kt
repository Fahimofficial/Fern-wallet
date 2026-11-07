package com.ivy.data.model

/**
 * How an account looks beyond its colour and icon: a logo and a card background.
 * [Default] is exactly how accounts looked before this existed (a solid card in the account's
 * colour, no logo), and is what every existing account and old backup decodes to.
 */
@Suppress("DataClassDefaultValues") // each default is the original look; Default is all of them
data class AccountAppearance(
    /** Id of a built-in logo (see the logo registry in the UI), or null. */
    val logoKey: String? = null,
    /** App-private file of a logo the user uploaded; wins over [logoKey]. */
    val customLogoPath: String? = null,
    val backgroundStyle: AccountBackgroundStyle = AccountBackgroundStyle.Solid,
    /** ARGB colours of a [AccountBackgroundStyle.Gradient], start first; empty otherwise. */
    val backgroundColors: List<Int> = emptyList(),
    /** Gradient direction in degrees, 0 = left to right, clockwise. */
    val gradientAngle: Float = DefaultGradientAngle,
    /** App-private file of a [AccountBackgroundStyle.Image] background. */
    val backgroundImagePath: String? = null,
    /** 0..1 black scrim over an image background, so text on it stays readable. */
    val backgroundDim: Float = DefaultBackgroundDim,
) {
    /** Files this appearance owns, to delete when they are replaced or the account goes. */
    val files: List<String> get() = listOfNotNull(customLogoPath, backgroundImagePath)

    companion object {
        val Default = AccountAppearance()
        const val DefaultGradientAngle = 45f
        const val DefaultBackgroundDim = 0.55f
    }
}

enum class AccountBackgroundStyle {
    /** The neutral card surface, no account colour. */
    None,

    /** The account's colour (the original look). */
    Solid,
    Gradient,
    Image,
}
