package com.ivy.legacy.ui.component.account

import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.data.media.AccountMediaStore
import com.ivy.data.model.AccountAppearance
import com.ivy.ui.logo.AccountLogo
import com.ivy.ui.logo.hasAccountLogo
import com.ivy.wallet.ui.theme.components.ItemIconSDefaultIcon

/**
 * "Use account colours on transactions" (Settings > Appearance), collected once at the root.
 * Not static: toggling it recomposes only the chips that read it.
 */
val LocalAccountColorsInTransactions = compositionLocalOf { false }

/** Imports picked account images; provided by the Activity, null in previews and tests. */
val LocalAccountMediaStore = staticCompositionLocalOf<AccountMediaStore?> { null }

/**
 * The account's logo when it has one, otherwise its classic icon tinted with [tint]. Both take a
 * [size] square, so swapping one for the other never moves the text next to it.
 */
@Composable
fun AccountMark(
    appearance: AccountAppearance,
    name: String,
    color: Color,
    iconName: String?,
    tint: Color,
    @DrawableRes defaultIcon: Int,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    if (hasAccountLogo(appearance.logoKey, appearance.customLogoPath)) {
        AccountLogo(
            logoKey = appearance.logoKey,
            customLogoPath = appearance.customLogoPath,
            name = name,
            color = color,
            size = size,
            modifier = modifier,
        )
    } else {
        ItemIconSDefaultIcon(modifier = modifier, iconName = iconName, defaultIcon = defaultIcon, tint = tint)
    }
}

/**
 * Opens the system Photo Picker for one image (no storage permission). Null where there's no
 * Activity to host it (previews, screenshot tests).
 */
@Composable
fun rememberImagePicker(onPicked: (Uri) -> Unit): (() -> Unit)? {
    if (LocalActivityResultRegistryOwner.current == null) return null
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    return { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
