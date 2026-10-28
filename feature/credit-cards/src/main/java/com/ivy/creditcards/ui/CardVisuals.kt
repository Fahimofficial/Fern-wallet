package com.ivy.creditcards.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.data.model.creditcard.CardAppearance
import com.ivy.data.model.creditcard.CardNetwork
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.ui.R

/** Colors of the generic Fern Wallet card designs (never an issuer's official artwork). */
@Immutable
data class CardPalette(val start: Color, val end: Color, val accent: Color)

@Suppress("MagicNumber") // colour literals of the built-in card palettes
fun CardAppearance.palette(): CardPalette = when (this) {
    CardAppearance.FERN -> CardPalette(Color(0xFF0B5747), Color(0xFF13876A), Color(0xFF7FD3B7))
    CardAppearance.MIDNIGHT -> CardPalette(Color(0xFF1B1E2C), Color(0xFF2E3350), Color(0xFF9AA4FF))
    CardAppearance.OCEAN -> CardPalette(Color(0xFF0D4A73), Color(0xFF1A6FA3), Color(0xFF8FD0FF))
    CardAppearance.SUNSET -> CardPalette(Color(0xFF7E3511), Color(0xFFB4561B), Color(0xFFFFC08A))
    CardAppearance.GRAPHITE -> CardPalette(Color(0xFF26262A), Color(0xFF424247), Color(0xFFD0D0D6))
    CardAppearance.ROSE -> CardPalette(Color(0xFF6E1B3E), Color(0xFF9E2E59), Color(0xFFFFA9C9))
}

/** Representative account color for the card (used where the app shows account colors). */
fun CardAppearance.accountColor(): Color = palette().end

@Immutable
@Suppress("DataClassDefaultValues") // optional visual details
data class CardVisualData(
    val nickname: String,
    val issuerName: String?,
    val productName: String?,
    val network: CardNetwork?,
    val lastFour: String?,
    val appearance: CardAppearance,
    val closed: Boolean = false,
)

/**
 * Generic card illustration: Fern Wallet design + issuer monogram + network name as text.
 * Bank logos and network marks are trademarks; they're not bundled (see docs/Card-Catalogue.md),
 * so this never imitates a bank's card.
 */
@Composable
fun CreditCardVisual(
    data: CardVisualData,
    modifier: Modifier = Modifier,
) {
    val palette = data.appearance.palette()
    val description = buildString {
        append(data.nickname)
        data.issuerName?.let { append(", ").append(it) }
        data.network?.let { append(", ").append(it.displayName) }
        data.lastFour?.let { append(", ending in ").append(it.toCharArray().joinToString(" ")) }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CardAspectRatio)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(palette.start, palette.end)))
            .clearAndSetSemantics { contentDescription = description }
    ) {
        CardPattern(palette.accent)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IssuerMonogram(name = data.issuerName ?: data.nickname, size = 36.dp, onDark = true)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = data.issuerName ?: data.nickname,
                        style = UI.typo.b2.style(color = Color.White, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    data.productName?.let {
                        Text(
                            text = it,
                            style = UI.typo.c.style(color = Color.White.copy(alpha = 0.8f)),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Column {
                Text(
                    text = data.lastFour?.let { "••••  ••••  ••••  $it" } ?: "••••  ••••  ••••  ••••",
                    style = UI.typo.nB1.style(color = Color.White, fontWeight = FontWeight.Bold)
                        .copy(letterSpacing = 1.sp),
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        modifier = Modifier.weight(1f),
                        text = data.nickname,
                        style = UI.typo.b2.style(color = Color.White, fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    data.network?.takeIf { it != CardNetwork.OTHER }?.let { NetworkLabel(it, onDark = true) }
                }
            }
        }
        if (data.closed) ClosedOverlay()
    }
}

@Composable
private fun BoxScope.CardPattern(accent: Color) {
    Canvas(Modifier.matchParentSize()) {
        val stroke = Stroke(width = 1.5.dp.toPx())
        val center = Offset(size.width * PatternCenterX, size.height * PatternCenterY)
        for (i in 1..PatternRings) {
            drawCircle(
                color = accent.copy(alpha = 0.10f),
                radius = size.height * (PatternBaseRadius + i * PatternRingStep),
                center = center,
                style = stroke
            )
        }
    }
}

@Composable
private fun BoxScope.ClosedOverlay() {
    Box(
        modifier = Modifier
            .matchParentSize()
            .background(Color.Black.copy(alpha = 0.45f))
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .border(1.5.dp, Color.White, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            text = stringResource(R.string.cc_card_closed_badge),
            style = UI.typo.b2.style(color = Color.White, fontWeight = FontWeight.ExtraBold)
        )
    }
}

/** Network name as plain text (not the trademark logo). */
@Composable
fun NetworkLabel(network: CardNetwork, onDark: Boolean, modifier: Modifier = Modifier) {
    val color = if (onDark) Color.White else UI.colors.pureInverse
    Text(
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
        text = network.displayName,
        style = UI.typo.c.style(color = color, fontWeight = FontWeight.ExtraBold)
    )
}

/** Initials badge for an issuer; a neutral stand-in for bank logos. */
@Composable
fun IssuerMonogram(
    name: String,
    size: Dp,
    onDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val bg = if (onDark) Color.White.copy(alpha = 0.18f) else UI.colors.medium
    val fg = if (onDark) Color.White else UI.colors.pureInverse
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / MonogramCornerDivisor))
            .background(bg)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram(name),
            style = UI.typo.c.style(color = fg, fontWeight = FontWeight.ExtraBold)
                .copy(fontSize = (size.value * 0.32f).sp)
        )
    }
}

private val IGNORED_WORDS = setOf("bank", "ltd", "ltd.", "limited", "the", "of", "and", "&", "co", "corp")
private const val MaxMonogram = 3
private const val ShortName = 4

fun monogram(name: String): String {
    val words = name.split(' ', '-', '.').filter { it.isNotBlank() && it.lowercase() !in IGNORED_WORDS }
    val first = words.firstOrNull() ?: return name.take(1).uppercase()
    return if (first.length <= ShortName && first.all { it.isUpperCase() || it.isDigit() }) {
        first
    } else {
        words.take(MaxMonogram).joinToString("") { it.take(1) }.uppercase()
    }
}

private const val CardAspectRatio = 1.586f // ISO/IEC 7810 ID-1
private const val PatternCenterX = 1.05f
private const val PatternCenterY = -0.1f
private const val PatternRings = 5
private const val PatternBaseRadius = 0.35f
private const val PatternRingStep = 0.22f
private const val MonogramCornerDivisor = 4
