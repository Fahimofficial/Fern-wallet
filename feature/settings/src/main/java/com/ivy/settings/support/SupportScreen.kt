package com.ivy.settings.support

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
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
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyToolbar

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
 * "Support My Work": the UPI QR code and actions, then a link to more ways to pay.
 * Reached from Help & Support and from the Home menu.
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

        Spacer(Modifier.height(24.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.support_optional_note),
            style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal),
        )

        Spacer(Modifier.height(48.dp))
    }
}

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
