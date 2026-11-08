package com.ivy.legacy.ui.component.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.ivy.base.media.AccountMedia
import com.ivy.data.model.AccountAppearance
import com.ivy.data.model.AccountBackgroundStyle
import com.ivy.design.system.appearance.cardInk
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.scrimAlphaForWhiteText
import com.ivy.wallet.ui.theme.dynamicContrast
import com.ivy.wallet.ui.theme.findContrastTextColor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** What an account card is painted with, and the ink (text/icon colour) that reads on it. */
@Immutable
@Suppress("DataClassDefaultValues") // secondary ink is the main ink unless a style says otherwise
data class AccountPaint(
    val style: AccountBackgroundStyle,
    val colors: List<Color>,
    val angle: Float,
    val imagePath: String?,
    val scrimAlpha: Float,
    val ink: Color,
    /** For secondary text (excluded label, converted balance). */
    val secondaryInk: Color = ink,
)

/**
 * Resolves an account's background: Solid (the original look) is the account colour, None is
 * [surface], a Gradient its stops, an Image its file under a black scrim. The ink is black or
 * white with at least 4.5:1 everywhere on the card; a gradient that neither reaches gets the
 * least scrim that makes white do, and an image always gets the scrim that holds over pure white.
 */
fun accountPaint(appearance: AccountAppearance, accountColor: Color, surface: Color): AccountPaint {
    val style = appearance.backgroundStyle
    return when (style) {
        AccountBackgroundStyle.None -> solidPaint(style, surface)
        // The original look, pixel for pixel: legacy text colours on the account colour.
        AccountBackgroundStyle.Solid -> solidPaint(style, accountColor).copy(
            ink = findContrastTextColor(accountColor),
            secondaryInk = accountColor.dynamicContrast(),
        )
        AccountBackgroundStyle.Gradient -> {
            val stops = appearance.backgroundColors.map(::Color).takeIf { it.size >= 2 } ?: listOf(accountColor)
            val ink = cardInk(stops)
            AccountPaint(style, stops, appearance.gradientAngle, null, ink.scrimAlpha, ink.content)
        }

        AccountBackgroundStyle.Image -> {
            val minScrim = scrimAlphaForWhiteText(listOf(Color.White))
            AccountPaint(
                style = style,
                colors = listOf(accountColor),
                angle = 0f,
                imagePath = appearance.backgroundImagePath,
                scrimAlpha = max(appearance.backgroundDim, minScrim),
                ink = Color.Black.contentColor(),
            )
        }
    }
}

private fun solidPaint(style: AccountBackgroundStyle, color: Color): AccountPaint = AccountPaint(
    style = style,
    colors = listOf(color),
    angle = 0f,
    imagePath = null,
    scrimAlpha = 0f,
    ink = color.contentColor(),
)

/** The colour(s) that stand for the account elsewhere (chips): a gradient's stops, else its colour. */
fun AccountAppearance.primaryColors(accountColor: Color): List<Color> =
    if (backgroundStyle == AccountBackgroundStyle.Gradient && backgroundColors.size >= 2) {
        backgroundColors.map(::Color)
    } else {
        listOf(accountColor)
    }

/**
 * Paints [paint] behind the content. The gradient brush is built once per size and colours,
 * and an image is decoded (downscaled, off the main thread) and cached by Coil, so cards scroll
 * without rework. Until an image is there, or if its file is missing, the account colour shows
 * under the same scrim, so the ink stays readable.
 */
@Composable
fun Modifier.accountBackground(paint: AccountPaint): Modifier {
    val painter = paint.imagePath?.let { path ->
        val context = LocalContext.current
        val file = remember(path) { AccountMedia.ownedFile(context, path) }
        file?.let {
            rememberAsyncImagePainter(
                remember(file) { ImageRequest.Builder(context).data(file).size(ImageMaxPx).build() }
            )
        }
    }
    return drawWithCache {
        val brush = if (paint.colors.size >= 2) {
            val (start, end) = gradientEnds(size, paint.angle)
            Brush.linearGradient(paint.colors, start = start, end = end)
        } else {
            null
        }
        onDrawBehind {
            if (brush != null) drawRect(brush) else drawRect(paint.colors.first())
            painter?.let { drawCropped(it) }
            if (paint.scrimAlpha > 0f) drawRect(Color.Black, alpha = paint.scrimAlpha)
        }
    }
}

// Line through the centre along the angle, long enough to span the whole rectangle.
private fun gradientEnds(size: Size, angleDegrees: Float): Pair<Offset, Offset> {
    val radians = Math.toRadians(angleDegrees.toDouble())
    val dx = cos(radians).toFloat()
    val dy = sin(radians).toFloat()
    val half = (abs(size.width * dx) + abs(size.height * dy)) / 2f
    val center = Offset(size.width / 2f, size.height / 2f)
    return Offset(center.x - dx * half, center.y - dy * half) to Offset(center.x + dx * half, center.y + dy * half)
}

// ContentScale.Crop: fill the card, centred, cut the overflow.
private fun DrawScope.drawCropped(painter: Painter) {
    val intrinsic = painter.intrinsicSize
    if (intrinsic.isUnspecified() || intrinsic.width <= 0f || intrinsic.height <= 0f) return
    val scale = max(size.width / intrinsic.width, size.height / intrinsic.height)
    val drawn = Size(intrinsic.width * scale, intrinsic.height * scale)
    clipRect {
        translate((size.width - drawn.width) / 2f, (size.height - drawn.height) / 2f) {
            with(painter) { draw(drawn) }
        }
    }
}

private fun Size.isUnspecified(): Boolean = this == Size.Unspecified

private const val ImageMaxPx = 1080
