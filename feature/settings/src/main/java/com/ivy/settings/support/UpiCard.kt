package com.ivy.settings.support

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ivy.base.legacy.Theme
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.White
import kotlinx.coroutines.delay

private const val CopiedFeedbackDurationMs = 2_000L

// About 13 px per QR module on a typical phone: still easy to scan from another phone.
private val QrDisplaySize = 140.dp
private val QrTilePadding = 8.dp

// Enlarged QR: the white tile is at most 420dp, and always fits the window with the Close button.
private val EnlargedCardPadding = 20.dp
private const val EnlargedWidthReservedDp = 80 // card padding and a margin on each side
private const val EnlargedHeightReservedDp = 380 // card padding, gaps, UPI ID and payee, Close button, system bars
private const val EnlargedMinSideDp = 160
private const val EnlargedMaxSideDp = 420

@Composable
internal fun UpiCard(actions: SupportScreenActions) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .padding(16.dp),
    ) {
        // The QR code shares a row with the heading and scan hint, so the card needs no full-width QR block.
        Row(verticalAlignment = Alignment.CenterVertically) {
            UpiPayeePaymentQrCodeImage()
            Spacer(Modifier.width(16.dp))
            UpiCardHeader(Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))

        UpiIdentityDetails()
        Spacer(Modifier.height(12.dp))

        UpiActionsButtons(actions)

        Spacer(Modifier.height(12.dp))
        UpiVerificationHint()
    }
}

@Composable
private fun UpiCardHeader(modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            modifier = Modifier.semantics { heading() },
            text = stringResource(R.string.support_upi),
            style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.support_upi_scan_hint),
            style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
private fun UpiIdentityDetails() {
    Column {
        Text(
            text = stringResource(R.string.support_upi_id_label),
            style = UI.typo.c.style(color = UI.colors.gray, fontWeight = FontWeight.Bold),
        )
        SelectionContainer {
            Text(
                text = SupportDestinations.UPI_ID,
                style = UI.typo.nB1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
            )
        }
        Text(
            text = stringResource(R.string.support_upi_payee, SupportDestinations.UPI_PAYEE_NAME),
            style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
        )
    }
}

@Composable
private fun UpiActionsButtons(actions: SupportScreenActions) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CopyUpiIdButton(onCopy = actions.onCopyUpiId)
            SupportPillButton(
                icon = R.drawable.ic_vue_support_heart,
                text = stringResource(R.string.support_open_upi_app),
                primary = true,
                onClick = actions.onOpenUpiApp,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SupportPillButton(
                icon = R.drawable.ic_share,
                text = stringResource(R.string.support_share_qr),
                onClick = actions.onShareQr,
            )
            SupportPillButton(
                icon = R.drawable.ic_save,
                text = stringResource(R.string.support_save_qr),
                onClick = actions.onSaveQr,
            )
        }
    }
}

@Composable
private fun UpiVerificationHint() {
    Text(
        text = stringResource(R.string.support_upi_verify_hint, SupportDestinations.UPI_PAYEE_NAME),
        style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
    )
}

/** Test tag of the small QR code that opens the enlarged one. */
internal const val UpiQrTag = "support_upi_qr"

private fun createUpiQrBitmap(): ImageBitmap =
    UpiQrCode.toBitmap(UpiQrCode.encode(SupportDestinations.upiPaymentUri())).asImageBitmap()

// A white tile and unfiltered scaling keep the QR modules sharp and scanner-readable.
@Composable
private fun UpiQrTile(bitmap: ImageBitmap, imageSize: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(QrTilePadding),
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(imageSize),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
        )
    }
}

@Composable
private fun UpiPayeePaymentQrCodeImage(modifier: Modifier = Modifier) {
    var enlarged by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(
        R.string.support_upi_qr_description,
        SupportDestinations.UPI_ID,
        SupportDestinations.UPI_PAYEE_NAME,
    )
    val qrBitmap = remember { createUpiQrBitmap() }
    UpiQrTile(
        bitmap = qrBitmap,
        imageSize = QrDisplaySize,
        modifier = modifier
            .testTag(UpiQrTag)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.support_enlarge_qr)) {
                enlarged = true
            }
            .semantics(mergeDescendants = true) { contentDescription = description },
    )
    if (enlarged) {
        UpiQrEnlargedDialog(onClose = { enlarged = false })
    }
}

/** The QR code at a size that scans from across a table, with Close below it. Back also closes it. */
@Composable
private fun UpiQrEnlargedDialog(onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        UpiQrEnlarged(onClose = onClose)
    }
}

@Composable
internal fun UpiQrEnlarged(onClose: () -> Unit) {
    val config = LocalConfiguration.current
    // Fits the narrower side of the window, and leaves room for the Close button in landscape.
    val side = minOf(config.screenWidthDp - EnlargedWidthReservedDp, config.screenHeightDp - EnlargedHeightReservedDp)
        .coerceIn(EnlargedMinSideDp, EnlargedMaxSideDp)
        .dp
    val description = stringResource(
        R.string.support_upi_qr_description,
        SupportDestinations.UPI_ID,
        SupportDestinations.UPI_PAYEE_NAME,
    )
    val qrBitmap = remember { createUpiQrBitmap() }
    Column(
        modifier = Modifier
            .width(side + EnlargedCardPadding * 2)
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .padding(EnlargedCardPadding),
    ) {
        UpiQrTile(
            bitmap = qrBitmap,
            imageSize = side - QrTilePadding * 2,
            modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
        )
        Spacer(Modifier.height(16.dp))
        // So the payer can check who they are paying while scanning.
        UpiIdentityDetails()
        Spacer(Modifier.height(16.dp))
        Row {
            SupportPillButton(
                icon = R.drawable.ic_outline_clear_24,
                text = stringResource(R.string.close),
                primary = true,
                onClick = onClose,
            )
        }
    }
}

@Composable
private fun CenteredUpiQrEnlarged(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { UpiQrEnlarged(onClose = {}) }
}

/** For screenshot testing */
@Composable
fun UpiQrEnlargedUiTest(isDark: Boolean) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        CenteredUpiQrEnlarged()
    }
}

@Composable
private fun RowScope.CopyUpiIdButton(onCopy: () -> Unit) {
    var copied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedFeedbackDurationMs)
            copied = false
        }
    }
    SupportPillButton(
        icon = if (copied) R.drawable.ic_check else R.drawable.ic_copy,
        text = stringResource(if (copied) R.string.support_upi_id_copied else R.string.support_copy_upi_id),
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        onClick = {
            onCopy()
            copied = true
        },
    )
}

@Composable
private fun RowScope.SupportPillButton(
    @DrawableRes icon: Int,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
) {
    val background = if (primary) UI.colors.primary else UI.colors.pure
    val content = if (primary) White else UI.colors.pureInverse
    Row(
        modifier = modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .clip(UI.shapes.rFull)
            .background(background, UI.shapes.rFull)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = icon to text,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "support_button_content",
        ) { (shownIcon, shownText) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(shownIcon),
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                // Wraps instead of truncating, so large font sizes still show the whole label.
                Text(
                    text = shownText,
                    style = UI.typo.c.style(color = content, fontWeight = FontWeight.Bold),
                )
            }
        }
    }
}
