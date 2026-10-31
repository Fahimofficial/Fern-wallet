package com.ivy.creditcards.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.design.system.motion.IvyMotion
import com.ivy.wallet.ui.theme.Ivy

/** Root of every card screen: own background + system-bar insets (non-legacy screen). */
@Composable
fun CardScreen(
    title: String,
    onBack: () -> Unit,
    backDescription: String,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
    bottomBar: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(UI.colors.pure)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconAction(icon = com.ivy.ui.R.drawable.ic_back_android, description = backDescription, onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                text = title,
                style = UI.typo.b1.style(color = UI.colors.pureInverse, fontWeight = FontWeight.ExtraBold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            actions()
        }
        Column(Modifier.weight(1f)) { content() }
        bottomBar?.invoke()
    }
}

@Composable
fun IconAction(
    @DrawableRes icon: Int,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(48.dp) // minimum touch target
            .clip(CircleShape)
            .clickable(role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = UI.colors.pureInverse,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 8.dp)
            .semantics { heading() },
        text = text,
        style = UI.typo.b2.style(color = UI.colors.gray, fontWeight = FontWeight.Bold)
    )
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(UI.shapes.r4)
            .background(UI.colors.medium)
            .padding(16.dp),
        content = content
    )
}

/** Label + value line used in summaries and statements. */
@Composable
fun AmountLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    emphasize: Boolean = false,
    valueColor: Color = UI.colors.pureInverse,
    note: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                style = UI.typo.b2.style(
                    color = if (emphasize) UI.colors.pureInverse else UI.colors.gray,
                    fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium
                )
            )
            note?.let {
                Text(text = it, style = UI.typo.nC.style(color = UI.colors.gray))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = value,
            textAlign = TextAlign.End,
            style = (if (emphasize) UI.typo.nB1 else UI.typo.nB2).style(
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Text(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .background(if (enabled) Ivy else UI.colors.gray.copy(alpha = 0.4f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        text = text,
        textAlign = TextAlign.Center,
        style = UI.typo.b2.style(color = Color.White, fontWeight = FontWeight.Bold)
    )
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = UI.colors.pureInverse,
) {
    Text(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clip(CircleShape)
            .border(1.dp, color.copy(alpha = ChipBorderAlpha), CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        text = text,
        textAlign = TextAlign.Center,
        style = UI.typo.b2.style(color = color, fontWeight = FontWeight.Bold)
    )
}

/** Selectable pill used for options (network, appearance, filters...). */
@Composable
fun ChoiceChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(if (selected) Ivy else UI.colors.medium)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        text = text,
        style = UI.typo.c.style(
            color = if (selected) Color.White else UI.colors.pureInverse,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    prefix: String? = null,
) {
    // Label sits above the field instead of floating in the outline notch: it stays
    // readable at large font scales and never collides with the border.
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 8.dp),
            text = label,
            style = UI.typo.nC.style(
                color = if (error != null) MaterialTheme.colorScheme.error else UI.colors.gray,
                fontWeight = FontWeight.Bold
            )
        )
        OutlinedTextField(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
                .fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            prefix = prefix?.let { { Text(it) } },
            singleLine = singleLine,
            isError = error != null,
            supportingText = (error ?: supportingText)?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            textStyle = UI.typo.b2.style(color = UI.colors.pureInverse, fontWeight = FontWeight.SemiBold),
            shape = UI.shapes.r4,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ivy,
                focusedLabelColor = Ivy,
                cursorColor = Ivy,
                unfocusedTextColor = UI.colors.pureInverse,
                focusedTextColor = UI.colors.pureInverse,
                errorBorderColor = MaterialTheme.colorScheme.error,
            )
        )
    }
}

/** Small screen-wide note, e.g. "calculated by Fern Wallet, not an official statement". */
@Composable
fun Disclaimer(text: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        text = text,
        style = UI.typo.nC.style(color = UI.colors.gray)
    )
}

/**
 * Loading state of a card screen. Loads are usually a few milliseconds, so the indicator fades in
 * only after a short moment instead of flashing; the screen keeps its title and background meanwhile.
 */
@Composable
fun ColumnScope.CardLoading(modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, IvyMotion.standard(IvyMotion.Duration.Medium, delayMillis = LoadingFadeInDelay))
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .weight(1f)
            .semantics { contentDescription = "Loading" },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer { this.alpha = alpha.value },
            color = Ivy,
            strokeWidth = 3.dp,
        )
    }
}

@Composable
fun VerticalSpace(height: Dp, modifier: Modifier = Modifier) {
    Spacer(modifier.height(height))
}

private const val ChipBorderAlpha = 0.4f
private const val LoadingFadeInDelay = 150
