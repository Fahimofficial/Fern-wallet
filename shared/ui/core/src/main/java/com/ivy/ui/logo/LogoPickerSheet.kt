package com.ivy.ui.logo

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.motion.IvyMotion
import com.ivy.ui.R

/** What the user chose in [LogoPickerSheet]. */
sealed interface LogoChoice {
    data class Registry(val key: String) : LogoChoice
    data object Upload : LogoChoice
    data object Initials : LogoChoice
    data object None : LogoChoice
}

/**
 * Bottom sheet to choose an account's logo: search (name or alias), category chips, a grid of
 * every logo, and Upload / Use initials / None. The current choice carries a check that scales in.
 */
private data class LogoPickerGridState(
    val results: List<LogoSpec>,
    val accountName: String,
    val accountColor: Color,
    val selectedKey: String?,
    val hasCustomLogo: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogoPickerSheet(
    accountName: String,
    accountColor: Color,
    selectedKey: String?,
    hasCustomLogo: Boolean,
    onChoice: (LogoChoice) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryName by rememberSaveable { mutableStateOf<String?>(null) }
    val category = LogoCategory.entries.firstOrNull { it.name == categoryName }
    val results = remember(query, category) { Logos.search(query, category) }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        LogoPickerHeader(
            hasCustomLogo = hasCustomLogo,
            query = query,
            onQueryChange = { query = it },
            onChoice = onChoice,
        )
        Spacer(Modifier.height(8.dp))
        LogoPickerCategories(category) { categoryName = it?.name }
        LogoPickerGridContent(
            state = LogoPickerGridState(
                results = results,
                accountName = accountName,
                accountColor = accountColor,
                selectedKey = selectedKey,
                hasCustomLogo = hasCustomLogo,
            ),
            onChoice = onChoice,
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LogoPickerHeader(
    hasCustomLogo: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onChoice: (LogoChoice) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(text = stringResource(R.string.logo), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onChoice(LogoChoice.Upload) }, modifier = Modifier.testTag("logo_upload")) {
                Text(stringResource(if (hasCustomLogo) R.string.logo_replace_upload else R.string.logo_upload))
            }
            OutlinedButton(onClick = { onChoice(LogoChoice.Initials) }) {
                Text(stringResource(R.string.logo_use_initials))
            }
            OutlinedButton(onClick = { onChoice(LogoChoice.None) }) {
                Text(stringResource(R.string.logo_none))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth().testTag("logo_search"),
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text(stringResource(R.string.logo_search_hint)) },
        )
    }
}

@Composable
private fun LogoPickerCategories(
    selectedCategory: LogoCategory?,
    onCategorySelected: (LogoCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text(stringResource(R.string.logo_category_all)) },
            )
        }
        items(LogoCategory.entries, key = { it.name }) { entry ->
            FilterChip(
                selected = selectedCategory == entry,
                onClick = { onCategorySelected(entry) },
                label = { Text(stringResource(entry.label())) },
            )
        }
    }
}

@Composable
private fun LogoPickerGridContent(
    state: LogoPickerGridState,
    onChoice: (LogoChoice) -> Unit,
) {
    LazyVerticalGrid(
        modifier = Modifier.heightIn(max = 480.dp),
        columns = GridCells.Adaptive(minSize = 80.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(state.results, key = { it.id }, contentType = { "logo" }) { spec ->
            LogoCell(
                spec = spec,
                accountName = state.accountName,
                accountColor = state.accountColor,
                selected = spec.id == state.selectedKey && !state.hasCustomLogo,
            ) { onChoice(LogoChoice.Registry(spec.id)) }
        }
    }
    if (state.results.isEmpty()) {
        Text(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            text = stringResource(R.string.logo_no_results),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LogoCell(
    spec: LogoSpec,
    accountName: String,
    accountColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp)
            .testTag("logo_${spec.id}"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            AccountLogo(
                logoKey = spec.id,
                customLogoPath = null,
                name = accountName,
                color = accountColor,
                size = 48.dp,
            )
            val accent = MaterialTheme.colorScheme.primary
            androidx.compose.animation.AnimatedVisibility(
                modifier = Modifier.align(Alignment.BottomEnd),
                visible = selected,
                enter = scaleIn(IvyMotion.Springs.bouncy()) + fadeIn(IvyMotion.standard(IvyMotion.Duration.Short)),
                exit = scaleOut(IvyMotion.standard(IvyMotion.Duration.Short)) +
                    fadeOut(IvyMotion.standard(IvyMotion.Duration.Short)),
            ) {
                Icon(
                    modifier = Modifier
                        .size(20.dp)
                        .background(accent, CircleShape)
                        .padding(2.dp),
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = accent.contentColor(),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = spec.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LogoCategory.label(): Int = when (this) {
    LogoCategory.Banks -> R.string.logo_category_banks
    LogoCategory.Wallets -> R.string.logo_category_wallets
    LogoCategory.Investing -> R.string.logo_category_investing
    LogoCategory.Cards -> R.string.logo_category_cards
    LogoCategory.Global -> R.string.logo_category_global
    LogoCategory.Generic -> R.string.logo_category_generic
}
