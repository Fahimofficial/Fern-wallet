package com.ivy.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.appicon.AppIcon
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.Ivy

@DrawableRes
private fun AppIcon.preview(): Int = when (this) {
    AppIcon.Default -> R.drawable.fern_icon_default
    AppIcon.Mono -> R.drawable.fern_icon_mono
    AppIcon.Graphite -> R.drawable.fern_icon_graphite
    AppIcon.Midnight -> R.drawable.fern_icon_midnight
}

@StringRes
private fun AppIcon.label(): Int = when (this) {
    AppIcon.Default -> R.string.app_icon_default
    AppIcon.Mono -> R.string.app_icon_mono
    AppIcon.Graphite -> R.string.app_icon_graphite
    AppIcon.Midnight -> R.string.app_icon_midnight
}

/** "App icon" setting: one tap switches the launcher icon. */
@Composable
internal fun AppIconPicker(
    selected: AppIcon,
    onSelect: (AppIcon) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .border(1.dp, UI.colors.medium, UI.shapes.r4)
            .padding(vertical = 16.dp)
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 20.dp),
            text = stringResource(R.string.app_icon),
            style = UI.typo.b2.style(
                color = UI.colors.pureInverse,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            modifier = Modifier.padding(horizontal = 20.dp),
            text = stringResource(R.string.app_icon_description),
            style = UI.typo.nC.style(
                color = UI.colors.gray,
                fontWeight = FontWeight.Normal
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppIcon.entries.forEach { icon ->
                AppIconOption(
                    icon = icon,
                    selected = icon == selected,
                    onClick = { onSelect(icon) }
                )
            }
        }
    }
}

@Composable
private fun AppIconOption(
    icon: AppIcon,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(UI.shapes.r4)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier
                .border(2.dp, if (selected) Ivy else UI.colors.medium, CircleShape)
                .padding(4.dp)
                .size(56.dp)
                .clip(CircleShape),
            painter = painterResource(icon.preview()),
            // The text label below names the option.
            contentDescription = null
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(icon.label()),
            style = UI.typo.c.style(
                color = if (selected) UI.colors.pureInverse else UI.colors.gray,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        )
    }
}

/** For screenshot testing */
@Composable
fun AppIconPickerUiTest(isDark: Boolean) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        AppIconPicker(selected = AppIcon.Default, onSelect = {})
    }
}
