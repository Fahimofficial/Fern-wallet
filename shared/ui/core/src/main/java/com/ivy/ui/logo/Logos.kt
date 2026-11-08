package com.ivy.ui.logo

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector

enum class LogoCategory { Banks, Wallets, Investing, Cards, Global, Generic }

/** What a logo is drawn with. */
@Immutable
sealed interface LogoArt {
    /** A single-colour brand glyph (vector drawable), drawn on the brand colour. */
    data class Glyph(@DrawableRes val drawable: Int) : LogoArt

    /** A generic Material icon, drawn on the account's colour. */
    data class Vector(val image: ImageVector) : LogoArt

    /** The brand's initials on its colour: used where no artwork could be licensed cleanly. */
    data object Initials : LogoArt
}

/**
 * One entry of the logo registry.
 * @param brandColor ARGB of the badge, or null to use the account's colour (generic icons).
 */
@Immutable
@Suppress("DataClassTypedIDs") // the id is a stable string key stored in the database
data class LogoSpec(
    val id: String,
    val name: String,
    val aliases: List<String>,
    val category: LogoCategory,
    val art: LogoArt,
    val brandColor: Long?,
)

object Logos {
    /** Logo key meaning "the account name's initials on the account colour". */
    const val InitialsKey = "initials"

    val all: List<LogoSpec> = BrandLogos + GenericLogos

    private val byId = all.associateBy { it.id }

    fun find(id: String?): LogoSpec? = id?.let(byId::get)

    /** Matches name, id or any alias, ignoring case and spaces; blank matches everything. */
    fun search(query: String, category: LogoCategory? = null): List<LogoSpec> {
        val needle = query.normalized()
        return all.filter { spec ->
            (category == null || spec.category == category) &&
                (
                    needle.isEmpty() ||
                        (listOf(spec.name, spec.id) + spec.aliases).any { it.normalized().contains(needle) }
                    )
        }
    }

    private fun String.normalized(): String = lowercase().filter { it.isLetterOrDigit() }
}

/**
 * Up to two characters for a badge: the first letter or digit of the first two words
 * ("Bank of Baroda" -> "BB", "slice" -> "S", "811" -> "8"); "?" when there are none.
 */
fun initialsOf(name: String): String {
    val words = name.split(' ', '-', '_', '.').mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
    val significant = words.filterIndexed { index, c -> index == 0 || c.isUpperCase() || c.isDigit() }
        .ifEmpty { words }
    return significant.take(2).joinToString("").uppercase().ifEmpty { "?" }
}
