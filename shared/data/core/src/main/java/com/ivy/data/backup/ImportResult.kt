package com.ivy.data.backup

import kotlinx.collections.immutable.ImmutableList

@Suppress("DataClassDefaultValues")
data class ImportResult(
    val rowsFound: Int,
    val transactionsImported: Int,
    val accountsImported: Int,
    val categoriesImported: Int,
    val failedRows: ImmutableList<CSVRow>,
    /** Set when nothing could be imported; explains why. */
    val failure: ImportFailure? = null,
    /** True if a copy of the previous data was saved and the import can be undone. */
    val canUndo: Boolean = false,
)
