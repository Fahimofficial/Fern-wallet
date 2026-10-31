package com.ivy.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ivy.base.legacy.Theme
import com.ivy.data.backup.auto.AutoBackupFailure
import com.ivy.data.backup.auto.AutoBackupFrequency
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.legacy.IvyWalletPreview
import com.ivy.ui.R

@Immutable
data class AutoBackupUi(
    val folderName: String?,
    val frequency: AutoBackupFrequency,
    val passwordProtected: Boolean,
    /** Formatted date and time of the last successful automatic backup. */
    val lastBackup: String?,
    val lastFailure: AutoBackupFailure?,
    val busy: Boolean,
) {
    companion object {
        val Empty = AutoBackupUi(
            folderName = null,
            frequency = AutoBackupFrequency.OFF,
            passwordProtected = false,
            lastBackup = null,
            lastFailure = null,
            busy = false,
        )
    }
}

@Immutable
class AutoBackupActions(
    val onPickFolder: () -> Unit,
    val onSetFrequency: (AutoBackupFrequency) -> Unit,
    /** null removes the password. */
    val onSetPassword: (String?) -> Unit,
    val onBackUpNow: () -> Unit,
) {
    companion object {
        val None = AutoBackupActions({}, {}, {}, {})
    }
}

/** Settings → Automatic backup. Stateless; the folder picker lives in the screen. */
@Composable
internal fun AutoBackupSection(state: AutoBackupUi, actions: AutoBackupActions) {
    var frequencyDialog by rememberSaveable { mutableStateOf(false) }
    var passwordDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        SettingsSectionDivider(text = stringResource(R.string.auto_backup))
        Spacer(Modifier.height(8.dp))
        SectionText(stringResource(R.string.auto_backup_description, AutoBackupsKept))
        Spacer(Modifier.height(16.dp))

        SettingsDefaultButton(
            icon = R.drawable.ic_vue_files_folder,
            iconPadding = 6.dp,
            text = stringResource(R.string.auto_backup_folder),
            description = state.folderName ?: stringResource(R.string.auto_backup_folder_not_set),
            onClick = actions.onPickFolder,
        )
        Spacer(Modifier.height(12.dp))
        SettingsDefaultButton(
            icon = R.drawable.ic_vue_main_calendar,
            iconPadding = 6.dp,
            text = stringResource(R.string.auto_backup_frequency),
            description = stringResource(state.frequency.labelRes()),
            onClick = { frequencyDialog = true },
        )
        Spacer(Modifier.height(12.dp))
        SettingsDefaultButton(
            icon = R.drawable.ic_vue_security_key,
            iconPadding = 6.dp,
            text = stringResource(R.string.auto_backup_password),
            description = stringResource(
                if (state.passwordProtected) R.string.auto_backup_password_on else R.string.auto_backup_password_off
            ),
            onClick = { passwordDialog = true },
        )
        Spacer(Modifier.height(12.dp))
        SettingsDefaultButton(
            icon = R.drawable.ic_vue_security_shield,
            iconPadding = 8.dp,
            text = stringResource(if (state.busy) R.string.auto_backup_running else R.string.auto_backup_now),
            onClick = { if (!state.busy) actions.onBackUpNow() },
        )
        Spacer(Modifier.height(8.dp))
        val status = state.statusRes()
        val statusText = if (status == R.string.auto_backup_last) {
            stringResource(status, state.lastBackup.orEmpty())
        } else {
            stringResource(status)
        }
        SectionText(statusText)
    }

    if (frequencyDialog) {
        FrequencyDialog(
            selected = state.frequency,
            onSelect = {
                frequencyDialog = false
                actions.onSetFrequency(it)
            },
            onDismiss = { frequencyDialog = false },
        )
    }
    if (passwordDialog) {
        PasswordDialog(
            title = stringResource(R.string.auto_backup_password_title),
            text = stringResource(R.string.auto_backup_password_text),
            confirmLabel = stringResource(R.string.auto_backup_password_save),
            onConfirm = {
                passwordDialog = false
                actions.onSetPassword(it)
            },
            onDismiss = { passwordDialog = false },
        )
    }
}

internal const val AutoBackupsKept = 10

/** The status line's string resource; [AutoBackupUi.lastBackup] is its argument when it has one. */
private fun AutoBackupUi.statusRes(): Int = when (lastFailure) {
    AutoBackupFailure.FOLDER_UNAVAILABLE -> R.string.auto_backup_failed_folder
    AutoBackupFailure.KEY_UNAVAILABLE -> R.string.auto_backup_failed_key
    AutoBackupFailure.WRITE_FAILED -> R.string.auto_backup_failed_write
    null -> if (lastBackup != null) R.string.auto_backup_last else R.string.auto_backup_never
}

private fun AutoBackupFrequency.labelRes(): Int = when (this) {
    AutoBackupFrequency.OFF -> R.string.auto_backup_off
    AutoBackupFrequency.DAILY -> R.string.auto_backup_daily
    AutoBackupFrequency.WEEKLY -> R.string.auto_backup_weekly
}

@Composable
private fun SectionText(text: String) {
    Text(
        modifier = Modifier.padding(horizontal = 32.dp),
        text = text,
        style = UI.typo.nC.style(color = UI.colors.gray, fontWeight = FontWeight.Normal)
    )
}

@Composable
private fun FrequencyDialog(
    selected: AutoBackupFrequency,
    onSelect: (AutoBackupFrequency) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auto_backup_frequency)) },
        text = {
            Column(Modifier.selectableGroup()) {
                AutoBackupFrequency.entries.forEach { frequency ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = frequency == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(frequency) },
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = frequency == selected, onClick = null)
                        Text(modifier = Modifier.padding(start = 12.dp), text = stringResource(frequency.labelRes()))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cc_cancel)) }
        },
    )
}

/**
 * Asks for a new password twice. An empty password means "no password" and is passed as null.
 * Non-empty passwords must have at least [MinPasswordLength] characters.
 */
@Composable
internal fun PasswordDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var repeat by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text)
                Spacer(Modifier.height(12.dp))
                PasswordField(value = password, label = R.string.password_hint) {
                    password = it
                    error = null
                }
                Spacer(Modifier.height(8.dp))
                PasswordField(value = repeat, label = R.string.password_repeat_hint) {
                    repeat = it
                    error = null
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(text = stringResource(it), color = UI.colors.red)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = when {
                    password != repeat -> R.string.password_mismatch
                    password.isNotEmpty() && password.length < MinPasswordLength -> R.string.password_too_short
                    else -> null
                }
                if (error == null) onConfirm(password.ifEmpty { null })
            }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cc_cancel)) }
        },
    )
}

@Composable
internal fun PasswordField(value: String, label: Int, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

internal const val MinPasswordLength = 8

/** For screenshot testing */
@Composable
fun AutoBackupSectionUiTest(isDark: Boolean, modifier: Modifier = Modifier) {
    IvyWalletPreview(theme = if (isDark) Theme.DARK else Theme.LIGHT) {
        Column(modifier.verticalScroll(rememberScrollState())) {
            AutoBackupSection(
                state = AutoBackupUi(
                    folderName = "Fern backups",
                    frequency = AutoBackupFrequency.DAILY,
                    passwordProtected = true,
                    lastBackup = "30 Oct 2026, 20:15",
                    lastFailure = null,
                    busy = false,
                ),
                actions = AutoBackupActions.None,
            )
        }
    }
}
