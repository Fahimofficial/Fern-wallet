package com.ivy.importdata.csvimport.flow

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.data.backup.ImportFailure
import com.ivy.data.backup.ImportResult
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.importdata.csv.Spacer8
import com.ivy.legacy.utils.format
import com.ivy.navigation.CSVScreen
import com.ivy.navigation.navigation
import com.ivy.ui.R
import com.ivy.wallet.ui.theme.GradientIvy
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.Green
import com.ivy.wallet.ui.theme.Red
import com.ivy.wallet.ui.theme.White
import com.ivy.wallet.ui.theme.components.BackButton
import com.ivy.wallet.ui.theme.components.IvyDividerLine
import com.ivy.wallet.ui.theme.components.OnboardingButton
import kotlinx.collections.immutable.persistentListOf

@SuppressLint("ComposeModifierMissing")
@Composable
fun ImportResultUI(
    result: ImportResult,
    launchedFromOnboarding: Boolean,
    isManualCsvImport: Boolean = false,
    onTryAgain: (() -> Unit)? = null,
    onUndoImport: (() -> Unit)? = null,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        val nav = navigation()
        BackButton(
            modifier = Modifier.padding(start = 20.dp)
        ) {
            nav.onBackPressed()
        }

        Spacer(Modifier.height(24.dp))

        val failure = result.failure
        val importSuccess = failure == null && (
            result.transactionsImported > 0 && result.transactionsImported > result.rowsFound / 2 ||
                result.rowsFound == 0 && result.accountsImported > 0
            )
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = if (importSuccess) stringResource(R.string.success) else stringResource(R.string.failure),
            style = UI.typo.h2.style(
                fontWeight = FontWeight.Black,
                color = if (importSuccess) UI.colors.pureInverse else Red
            )
        )

        Spacer(Modifier.height(32.dp))

        if (failure != null) {
            Text(
                modifier = Modifier.padding(horizontal = 32.dp),
                text = stringResource(failure.messageRes()),
                style = UI.typo.b1.style(color = UI.colors.pureInverse)
            )
        } else {
            ImportCounts(
                result = result,
                isManualCsvImport = isManualCsvImport,
                launchedFromOnboarding = launchedFromOnboarding,
            )
        }

        if (onUndoImport != null && result.canUndo) {
            UndoImportSection(onUndoImport = onUndoImport)
        }

        Spacer(Modifier.weight(1f))

        Spacer8()

        OnboardingButton(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = stringResource(R.string.finish),
            textColor = White,
            backgroundGradient = GradientIvy,
            hasNext = true,
            enabled = true
        ) {
            onFinish()
        }

        if (onTryAgain != null) {
            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                onClick = onTryAgain,
                enabled = true
            ) {
                Text(text = stringResource(R.string.try_again))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

private fun ImportFailure.messageRes(): Int = when (this) {
    ImportFailure.NOT_A_BACKUP -> R.string.backup_error_not_a_backup
    ImportFailure.CORRUPTED -> R.string.backup_error_corrupted
    ImportFailure.TOO_LARGE -> R.string.backup_error_too_large
    ImportFailure.NEWER_VERSION -> R.string.backup_error_newer_version
    ImportFailure.SAVE_FAILED -> R.string.backup_error_save_failed
}

@Composable
private fun UndoImportSection(onUndoImport: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Column {
        Spacer(Modifier.height(24.dp))
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.undo_import_hint),
            style = UI.typo.nB2.style(color = Gray)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            onClick = { confirm = true }
        ) {
            Text(text = stringResource(R.string.undo_import))
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.undo_import_confirm_title)) },
            text = { Text(stringResource(R.string.undo_import_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    onUndoImport()
                }) { Text(stringResource(R.string.undo_import)) }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.cc_cancel)) }
            }
        )
    }
}

@Composable
private fun ImportCounts(
    result: ImportResult,
    isManualCsvImport: Boolean,
    launchedFromOnboarding: Boolean,
) {
    val nav = navigation()
    Column {
        val successPercent = if (result.rowsFound > 0) {
            (result.transactionsImported / result.rowsFound.toDouble()) * 100
        } else {
            0.0
        }

        SuccessSectionUI(
            result = result,
            successPercent = successPercent,
        )

        IvyDividerLine(
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        FailedSectionUI(
            result = result,
            successPercent = successPercent,
        )

        if (!isManualCsvImport) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                text = stringResource(R.string.csv_import_failed),
                color = UI.colors.pureInverse,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
                onClick = {
                    nav.navigateTo(CSVScreen(launchedFromOnboarding = launchedFromOnboarding))
                }
            ) {
                Text(
                    text = stringResource(id = R.string.manual_csv_import),
                    style = UI.typo.b2.style(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
        }
    }
}

@Composable
private fun SuccessSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.imported),
            style = UI.typo.b1.style(
                color = Green,
                fontWeight = FontWeight.Black
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${successPercent.format(2)}%",
            style = UI.typo.nH2.style(
                fontWeight = FontWeight.Normal
            )
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.transactions_imported, result.transactionsImported),
            style = UI.typo.nB2.style(
                fontWeight = FontWeight.Bold,
                color = Gray
            )
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.accounts_imported, result.accountsImported),
            style = UI.typo.nB2.style(
                fontWeight = FontWeight.Bold,
                color = Gray
            )
        )

        Spacer(Modifier.height(4.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.categories_imported, result.categoriesImported),
            style = UI.typo.nB2.style(
                fontWeight = FontWeight.Bold,
                color = Gray
            )
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun FailedSectionUI(
    result: ImportResult,
    successPercent: Double,
) {
    Column {
        Spacer(Modifier.height(32.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(R.string.failed),
            style = UI.typo.b1.style(
                fontWeight = FontWeight.Black,
                color = Red
            )
        )

        Spacer(Modifier.height(8.dp))

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = "${(100 - successPercent).format(2)}%",
            style = UI.typo.nH2.style(
                fontWeight = FontWeight.Normal
            )
        )

        Text(
            modifier = Modifier.padding(horizontal = 32.dp),
            text = stringResource(
                R.string.rows_from_csv_not_recognized,
                result.rowsFound - result.transactionsImported
            ),
            style = UI.typo.nB2.style(
                fontWeight = FontWeight.Bold,
                color = Gray
            )
        )
    }
}

@Preview(device = "id:pixel_3", showBackground = true, showSystemUi = true)
@Composable
private fun Preview() {
    com.ivy.legacy.IvyWalletPreview {
        ImportResultUI(
            result = ImportResult(
                rowsFound = 356,
                transactionsImported = 320,
                accountsImported = 4,
                categoriesImported = 13,
                failedRows = persistentListOf()
            ),
            launchedFromOnboarding = false,
        ) {
        }
    }
}
