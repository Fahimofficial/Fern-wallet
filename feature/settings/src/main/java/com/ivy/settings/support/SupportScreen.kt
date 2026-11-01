package com.ivy.settings.support

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ivy.base.legacy.Theme
import com.ivy.base.links.SupportDestinations
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.rootScreen
import com.ivy.navigation.navigation
import com.ivy.settings.SettingsSectionDivider
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.IvyToolbar
import kotlinx.coroutines.delay
import com.ivy.settings.R as SettingsR

/** Ko-fi's brand red, used only to tint the Ko-fi cup. */
private val KofiRed = Color(0xFFFF5E5B)

private const val CopiedFeedbackMs = 2_000L
private val QrSize = 232.dp

/** Everything the Support screen can do. Plain lambdas keep the UI stateless and previewable. */
@Immutable
internal class SupportScreenActions(
    val onBack: () -> Unit,
    val onOpenUrl: (String) -> Unit,
    val onCopyUpiId: () -> Unit,
    val onOpenUpiApp: () -> Unit,
    val onShareQr: () -> Unit,
    val onSaveQr: () -> Unit,
)

/**
 * "Support my work": Ko-fi and UPI payment options, then this app's own project and contact links.
 *
 * Nothing is collected or stored. Paying always happens in another app the user picks, and this
 * screen never reports a payment as made.
 */
@Composable
fun SupportScreenImpl() {
    val nav = navigation()
    val rootScreen = rootScreen()
    val context = LocalContext.current
    val savedMessage = stringResource(R.string.support_qr_saved)
    val saveFailedMessage = stringResource(R.string.support_qr_save_failed)
    val shareFailedMessage = stringResource(R.string.support_share_qr_failed)
    val shareTitle = stringResource(R.string.support_share_qr)
    val shareText = stringResource(
        R.string.support_share_qr_text,
        SupportDestinations.UPI_ID,
        SupportDestinations.UPI_PAYEE_NAME,
    )

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(SupportQrExport.MIME_TYPE)
    ) { uri ->
        if (uri != null) {
            val saved = SupportQrExport.save(context, uri)
            Toast.makeText(context, if (saved) savedMessage else saveFailedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    SupportUi(
        actions = SupportScreenActions(
            onBack = { nav.onBackPressed() },
            onOpenUrl = { url -> rootScreen.openUrlInBrowser(url) },
            onCopyUpiId = {
                rootScreen.copyToClipboard(label = SupportDestinations.UPI_ID, text = SupportDestinations.UPI_ID)
            },
            onOpenUpiApp = { rootScreen.openUpiPayment(SupportDestinations.upiPaymentUri()) },
            onShareQr = {
                if (!SupportQrExport.share(context, shareTitle, shareText)) {
                    Toast.makeText(context, shareFailedMessage, Toast.LENGTH_SHORT).show()
                }
            },
            onSaveQr = { saveLauncher.launch(SupportQrExport.FILE_NAME) },
        )
    )
}

@Composable
private fun SupportUi(actions: SupportScreenActions) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        IvyToolbar(onBack = actions.onBack)

        Text(
            modifier = Modifier
                .padding(horizontal = 32.dp)
                .semantics { heading() },
            text = stringResource(R.string.support_my_work),
            style = UI.typo.h2.style(fontWeight = FontWeight.Black),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.support_intro),
            style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Normal)
                .copy(lineHeight = 22.sp),
        )

        PaymentOptionsSection(actions = actions)
        ProjectContactSection(actions = actions)

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun PaymentOptionsSection(actions: SupportScreenActions) {
    SettingsSectionDivider(text = stringResource(R.string.support_payment_options))
    Spacer(Modifier.height(16.dp))

    SupportLinkRow(
        icon = SettingsR.drawable.ic_kofi,
        iconTint = KofiRed,
        title = stringResource(R.string.support_kofi),
        description = SupportDestinations.KOFI_URL.withoutScheme(),
        onClick = { actions.onOpenUrl(SupportDestinations.KOFI_URL) },
    )

    Spacer(Modifier.height(12.dp))

    UpiCard(actions)
}

@Composable
private fun ProjectContactSection(actions: SupportScreenActions) {
    SettingsSectionDivider(text = stringResource(R.string.support_project_and_contact))
    Spacer(Modifier.height(8.dp))

    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = stringResource(R.string.support_project_info),
        style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
    )

    Spacer(Modifier.height(16.dp))

    SupportLinkRow(
        icon = R.drawable.github_logo,
        title = stringResource(R.string.support_app_repository),
        description = SupportDestinations.APP_REPOSITORY.withoutScheme(),
        onClick = { actions.onOpenUrl(SupportDestinations.APP_REPOSITORY) },
    )
    Spacer(Modifier.height(12.dp))
    SupportLinkRow(
        icon = R.drawable.ic_import_web,
        title = stringResource(R.string.support_website),
        description = SupportDestinations.WEBSITE.withoutScheme(),
        onClick = { actions.onOpenUrl(SupportDestinations.WEBSITE) },
    )
    Spacer(Modifier.height(12.dp))
    SupportLinkRow(
        icon = R.drawable.github_logo,
        title = stringResource(R.string.support_github_profile),
        description = SupportDestinations.GITHUB_PROFILE.withoutScheme(),
        onClick = { actions.onOpenUrl(SupportDestinations.GITHUB_PROFILE) },
    )
}

@Composable
private fun UpiCard(actions: SupportScreenActions) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        UpiCardHeader()
        Spacer(Modifier.height(16.dp))
        
        UpiQr(Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(16.dp))
        
        UpiIdentityDetails()
        Spacer(Modifier.height(16.dp))
        
        UpiActionsButtons(actions)
        
        Spacer(Modifier.height(16.dp))
        UpiVerificationHint()
    }
}

@Composable
private fun UpiCardHeader() {
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

@Composable
private fun UpiIdentityDetails() {
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

@Composable
private fun UpiActionsButtons(actions: SupportScreenActions) {
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

@Composable
private fun UpiVerificationHint() {
    Text(
        text = stringResource(R.string.support_upi_verify_hint, SupportDestinations.UPI_PAYEE_NAME),
        style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
    )
}

/**
 * The UPI QR code, drawn black on a white tile in both themes (scanners need dark-on-light).
 * Rendered at one pixel per module and scaled without filtering, so every module stays sharp.
 */
@Composable
private fun UpiQr(modifier: Modifier = Modifier) {
    val bitmap = remember {
        UpiQrCode.toBitmap(UpiQrCode.encode(SupportDestinations.upiPaymentUri())).asImageBitmap()
    }
    val description = stringResource(
        R.string.support_upi_qr_description,
        SupportDestinations.UPI_ID,
        SupportDestinations.UPI_PAYEE_NAME,
    )
    Box(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .padding(8.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier.size(QrSize),
            contentScale = ContentScale.FillBounds,
            filterQuality = FilterQuality.None,
        )
    }
}

@Composable
private fun RowScope.CopyUpiIdButton(onCopy: () -> Unit) {
    var copied by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(CopiedFeedbackMs)
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
                Text(
                    text = shownText,
                    style = UI.typo.c.style(color = content, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                )
            }
        }
    }
}

/** A Settings-style link row: same shape, colours and spacing as the rows in Settings. */
@Composable
private fun SupportLinkRow(
    @DrawableRes icon: Int,
    title: String,
    description: String,
    onClick: () -> Unit,
    iconTint: Color = UI.colors.pureInverse,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp),
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.Bold),
            )
            Text(
                text = description,
                style = UI.typo.nB2.style(color = UI.colors.gray, fontWeight = FontWeight.Normal)
                    .copy(fontSize = 14.sp),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            tint = UI.colors.gray,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun String.withoutScheme(): String = removePrefix("https://").removeSuffix("/")

/** For screenshot testing */
@Composable
fun SupportScreenUiTest(isDark: Boolean) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        SupportUi(
            SupportScreenActions(
                onBack = {},
                onOpenUrl = {},
                onCopyUpiId = {},
                onOpenUpiApp = {},
                onShareQr = {},
                onSaveQr = {},
            )
        )
    }
}
