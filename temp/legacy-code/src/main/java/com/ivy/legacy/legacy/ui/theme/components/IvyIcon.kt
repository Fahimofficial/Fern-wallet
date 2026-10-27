package com.ivy.wallet.ui.theme.components

import androidx.annotation.DrawableRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.ivy.design.l0_system.UI

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Composable
fun IvyIcon(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    tint: Color = UI.colors.pureInverse,
    // Decorative by default: icons next to text shouldn't be announced as "icon".
    contentDescription: String? = null
) {
    Icon(
        modifier = modifier,
        painter = painterResource(id = icon),
        contentDescription = contentDescription,
        tint = tint
    )
}
