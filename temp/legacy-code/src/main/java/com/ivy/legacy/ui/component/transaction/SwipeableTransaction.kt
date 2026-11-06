package com.ivy.legacy.ui.component.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.system.appearance.ColorToken
import com.ivy.design.system.appearance.FernTheme
import com.ivy.design.system.appearance.contentColor
import com.ivy.design.system.appearance.get
import com.ivy.design.system.motion.IvyHaptics
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.components.IvyIcon
import kotlinx.coroutines.flow.drop

/**
 * Swipe right to edit, left to delete (undoable, so no confirmation). The coloured action is
 * revealed under the card as it moves, a tick marks the point where releasing commits, and both
 * actions are TalkBack custom actions for people who can't swipe.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SwipeableTransaction(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Editing opens a screen: the card springs back behind it.
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }

                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }

                SwipeToDismissBoxValue.Settled -> true
            }
        },
    )
    LaunchedEffect(state) {
        snapshotFlow { state.targetValue }.drop(1).collect { target ->
            if (target != SwipeToDismissBoxValue.Settled) IvyHaptics.tick(view)
        }
    }

    val editLabel = stringResource(R.string.edit)
    val deleteLabel = stringResource(R.string.delete)
    SwipeToDismissBox(
        modifier = Modifier.semantics {
            customActions = listOf(
                CustomAccessibilityAction(editLabel) {
                    onEdit()
                    true
                },
                CustomAccessibilityAction(deleteLabel) {
                    onDelete()
                    true
                },
            )
        },
        state = state,
        backgroundContent = {
            val editing = state.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            val deleting = state.dismissDirection == SwipeToDismissBoxValue.EndToStart
            if (editing || deleting) {
                val color = FernTheme.palette[if (editing) ColorToken.Accent else ColorToken.Expense]
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Same insets as the card, so the colour shows only where the card was.
                        .padding(horizontal = 16.dp)
                        .padding(top = 10.dp)
                        .graphicsLayer { alpha = (state.progress * RevealSpeed).coerceIn(0f, 1f) }
                        .clip(UI.shapes.r4)
                        .background(color, UI.shapes.r4)
                        .padding(horizontal = 24.dp),
                    contentAlignment = if (editing) Alignment.CenterStart else Alignment.CenterEnd,
                ) {
                    IvyIcon(
                        icon = if (editing) R.drawable.ic_edit else R.drawable.ic_delete,
                        tint = color.contentColor(),
                    )
                }
            }
        },
    ) {
        content()
    }
}

// The colour is fully in by a third of the way, well before the commit point.
private const val RevealSpeed = 3f
