package com.ivy.data.repository.mapper

import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.model.AccountAppearance
import com.ivy.data.model.AccountBackgroundStyle

/** The appearance stored in the entity's nullable columns; unknown or missing values fall back. */
fun AccountEntity.appearance(): AccountAppearance = AccountAppearance(
    logoKey = logoKey,
    customLogoPath = customLogoPath,
    backgroundStyle = AccountBackgroundStyle.entries.firstOrNull { it.name == backgroundStyle }
        ?: AccountBackgroundStyle.Solid,
    backgroundColors = decodeColors(backgroundColors),
    gradientAngle = backgroundAngle ?: AccountAppearance.DefaultGradientAngle,
    backgroundImagePath = backgroundImagePath,
    backgroundDim = (backgroundDim ?: AccountAppearance.DefaultBackgroundDim).coerceIn(0f, 1f),
)

/** [appearance] in the entity's columns; the default writes nulls, as an untouched account has. */
fun AccountEntity.withAppearance(appearance: AccountAppearance): AccountEntity {
    if (appearance == AccountAppearance.Default) {
        return copy(
            logoKey = null,
            customLogoPath = null,
            backgroundStyle = null,
            backgroundColors = null,
            backgroundAngle = null,
            backgroundImagePath = null,
            backgroundDim = null,
        )
    }
    return copy(
        logoKey = appearance.logoKey,
        customLogoPath = appearance.customLogoPath,
        backgroundStyle = appearance.backgroundStyle.name,
        backgroundColors = encodeColors(appearance.backgroundColors),
        backgroundAngle = appearance.gradientAngle,
        backgroundImagePath = appearance.backgroundImagePath,
        backgroundDim = appearance.backgroundDim,
    )
}

internal fun encodeColors(colors: List<Int>): String? =
    colors.takeIf { it.isNotEmpty() }?.joinToString(",") { "%08X".format(it) }

internal fun decodeColors(value: String?): List<Int> =
    value?.split(",")?.mapNotNull { it.trim().toLongOrNull(radix = 16)?.toInt() }.orEmpty()
