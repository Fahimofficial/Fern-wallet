package com.ivy.wallet.domain.deprecated.logic.model

import androidx.compose.ui.graphics.Color
import com.ivy.data.model.AccountAppearance

@Suppress("DataClassDefaultValues") // callers that don't customise get the original look
data class CreateAccountData(
    val name: String,
    val currency: String,
    val color: Color,
    val icon: String?,
    val balance: Double,
    val includeBalance: Boolean = true,
    val appearance: AccountAppearance = AccountAppearance.Default,
)
