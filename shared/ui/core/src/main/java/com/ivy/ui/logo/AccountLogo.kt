package com.ivy.ui.logo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.ivy.base.media.AccountMedia
import com.ivy.design.system.appearance.contentColor

/** Whether an account has a logo to show instead of its plain icon. */
fun hasAccountLogo(logoKey: String?, customLogoPath: String?): Boolean =
    customLogoPath != null || logoKey == Logos.InitialsKey || Logos.find(logoKey) != null

/**
 * An account's logo as a round badge of exactly [size], so lists never shift when it loads.
 *
 * - An uploaded image ([customLogoPath], app-private only) is decoded off the main thread and
 *   cached by Coil; the initials show underneath until it's there, and stay if the file is gone
 *   (e.g. a backup restored on another device).
 * - A registry logo is its glyph on the brand colour, a generic icon on [color], or initials.
 * - [Logos.InitialsKey] is the account name's initials on [color].
 *
 * Glyphs and text pick black or white for contrast with what's under them.
 * @param contentDescription null when the account name is already next to it (chips, cards).
 */
@Composable
fun AccountLogo(
    logoKey: String?,
    customLogoPath: String?,
    name: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val spec = Logos.find(logoKey)
    val badge = spec?.brandColor?.let { Color(it) } ?: color
    val context = LocalContext.current
    val file = remember(customLogoPath) { AccountMedia.ownedFile(context, customLogoPath) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(badge, CircleShape)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier.clearAndSetSemantics { }
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        val tint = badge.contentColor()
        when (val art = spec?.art) {
            is LogoArt.Glyph -> Icon(
                modifier = Modifier.size(size * GlyphFraction),
                painter = painterResource(art.drawable),
                contentDescription = null,
                tint = tint,
            )

            is LogoArt.Vector -> Icon(
                modifier = Modifier.size(size * GlyphFraction),
                imageVector = art.image,
                contentDescription = null,
                tint = tint,
            )

            LogoArt.Initials, null -> Initials(spec?.name ?: name, tint, size)
        }
        if (file != null) {
            AsyncImage(
                modifier = Modifier.fillMaxSize(),
                model = remember(file) {
                    ImageRequest.Builder(context).data(file).crossfade(true).build()
                },
                contentDescription = null,
                contentScale = ContentScale.Crop,
            )
        }
    }
}

@Composable
private fun Initials(name: String, color: Color, size: Dp) {
    val fontSize = with(LocalDensity.current) { (size * InitialsFraction).toSp() }
    Text(
        text = initialsOf(name),
        style = TextStyle(color = color, fontSize = fontSize, fontWeight = FontWeight.Bold),
        maxLines = 1,
    )
}

private const val GlyphFraction = 0.58f
private const val InitialsFraction = 0.4f
