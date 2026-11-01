package com.ivy.ui.undo

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ivy.ui.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-wide "<something> deleted · Undo" message, shown by [UndoSnackbarHost] at the root so it
 * survives the screen that caused it closing (deleting a transaction closes its edit screen).
 *
 * Use it for reversible slips instead of a confirmation dialog: act immediately and offer Undo.
 * Keep confirmations for actions that can't be undone or that remove a lot at once.
 */
@Stable
@Singleton
class UndoController @Inject constructor() {
    var current: UndoMessage? by mutableStateOf(null)
        private set

    private var nextId = 0L

    /** Shows [text] with an Undo action, replacing any message still on screen. */
    fun show(
        @StringRes text: Int,
        @StringRes actionLabel: Int = R.string.undo,
        onUndo: suspend () -> Unit,
    ) {
        current = UndoMessage(id = nextId++, text = text, actionLabel = actionLabel, onUndo = onUndo)
    }

    /** Hides [message] if it's still the one shown (it may have been replaced meanwhile). */
    fun dismiss(message: UndoMessage) {
        if (current?.id == message.id) current = null
    }
}

@Immutable
class UndoMessage internal constructor(
    val id: Long,
    @StringRes val text: Int,
    @StringRes val actionLabel: Int,
    val onUndo: suspend () -> Unit,
)
