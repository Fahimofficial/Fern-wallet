package com.ivy.wallet.ui.applocked

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ivy.design.system.FernSpacing
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.utils.hasLockScreen
import com.ivy.ui.R

/**
 * Shown while the app is locked. Material 3 with Fern tokens: the reference for migrating
 * legacy screens (see docs/Design-System.md).
 */
@SuppressLint("ComposeModifierMissing")
@Composable
@Suppress("LongMethod", "FunctionNaming")
fun BoxWithConstraintsScope.AppLockedScreen(
    onShowOSBiometricsModal: () -> Unit,
    onOpenSecuritySettings: () -> Unit
) {
    val context = LocalContext.current
    // Without a screen lock there is nothing to authenticate with. Never unlock in that case:
    // ask the user to set one (re-checked every time this screen is shown or Unlock is tapped).
    var deviceSecure by remember { mutableStateOf(hasLockScreen(context)) }
    val latestOnOpenSecuritySettings by rememberUpdatedState(onOpenSecuritySettings)
    val latestOnShowOSBiometricsModal by rememberUpdatedState(onShowOSBiometricsModal)

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = FernSpacing.ScreenHorizontal, vertical = FernSpacing.XXLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = FernSpacing.XXLarge, vertical = FernSpacing.Medium)
                        .semantics { heading() },
                    text = stringResource(R.string.app_locked),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.weight(1f))

            Image(
                modifier = Modifier.size(width = 96.dp, height = 138.dp),
                painter = painterResource(id = R.drawable.ic_fingerprint),
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.outlineVariant),
                contentScale = ContentScale.FillBounds,
                contentDescription = null
            )

            Spacer(Modifier.weight(1f))

            Text(
                modifier = Modifier.padding(horizontal = FernSpacing.Large),
                text = stringResource(
                    if (deviceSecure) R.string.authenticate_text else R.string.app_lock_no_screen_lock
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(FernSpacing.XLarge))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium,
                onClick = {
                    deviceSecure = hasLockScreen(context)
                    if (deviceSecure) latestOnShowOSBiometricsModal() else latestOnOpenSecuritySettings()
                }
            ) {
                Text(
                    text = stringResource(if (deviceSecure) R.string.unlock else R.string.app_lock_open_settings),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }

    // Show the system prompt as soon as the screen appears.
    LaunchedEffect(Unit) {
        if (deviceSecure) latestOnShowOSBiometricsModal()
    }
    // Re-check after returning from the phone's settings. Doesn't prompt by itself: the
    // device-credential screen also pauses this activity, and cancelling it must not loop.
    LifecycleResumeEffect(Unit) {
        deviceSecure = hasLockScreen(context)
        onPauseOrDispose { }
    }
}

@Preview
@Composable
@Suppress("FunctionNaming", "UnusedPrivateMember")
private fun Preview_Locked() {
    IvyWalletPreview {
        AppLockedScreen(
            onOpenSecuritySettings = {},
            onShowOSBiometricsModal = {}
        )
    }
}
