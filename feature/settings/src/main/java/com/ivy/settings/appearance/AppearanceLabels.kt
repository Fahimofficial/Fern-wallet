package com.ivy.settings.appearance

import androidx.annotation.StringRes
import com.ivy.design.system.appearance.AppearancePreset
import com.ivy.design.system.appearance.ColorGroup
import com.ivy.design.system.appearance.ColorToken
import com.ivy.ui.R

// Exhaustive `when`s so a new token can't ship without a label.
@Suppress("CyclomaticComplexMethod")
@StringRes
internal fun ColorToken.nameRes(): Int = when (this) {
    ColorToken.Background -> R.string.appearance_token_background
    ColorToken.Card -> R.string.appearance_token_card
    ColorToken.Elevated -> R.string.appearance_token_elevated
    ColorToken.TextPrimary -> R.string.appearance_token_text_primary
    ColorToken.TextSecondary -> R.string.appearance_token_text_secondary
    ColorToken.Outline -> R.string.appearance_token_outline
    ColorToken.Accent -> R.string.appearance_token_accent
    ColorToken.AccentSecondary -> R.string.appearance_token_accent_secondary
    ColorToken.NavBar -> R.string.appearance_token_nav_bar
    ColorToken.NavSelected -> R.string.appearance_token_nav_selected
    ColorToken.NavUnselected -> R.string.appearance_token_nav_unselected
    ColorToken.Fab -> R.string.appearance_token_fab
    ColorToken.Income -> R.string.appearance_token_income
    ColorToken.Expense -> R.string.appearance_token_expense
    ColorToken.Transfer -> R.string.appearance_token_transfer
    ColorToken.Upcoming -> R.string.appearance_token_upcoming
    ColorToken.Overdue -> R.string.appearance_token_overdue
    ColorToken.BudgetProgress -> R.string.appearance_token_budget
    ColorToken.SavingsProgress -> R.string.appearance_token_savings
}

@Suppress("CyclomaticComplexMethod")
@StringRes
internal fun ColorToken.descriptionRes(): Int = when (this) {
    ColorToken.Background -> R.string.appearance_token_background_description
    ColorToken.Card -> R.string.appearance_token_card_description
    ColorToken.Elevated -> R.string.appearance_token_elevated_description
    ColorToken.TextPrimary -> R.string.appearance_token_text_primary_description
    ColorToken.TextSecondary -> R.string.appearance_token_text_secondary_description
    ColorToken.Outline -> R.string.appearance_token_outline_description
    ColorToken.Accent -> R.string.appearance_token_accent_description
    ColorToken.AccentSecondary -> R.string.appearance_token_accent_secondary_description
    ColorToken.NavBar -> R.string.appearance_token_nav_bar_description
    ColorToken.NavSelected -> R.string.appearance_token_nav_selected_description
    ColorToken.NavUnselected -> R.string.appearance_token_nav_unselected_description
    ColorToken.Fab -> R.string.appearance_token_fab_description
    ColorToken.Income -> R.string.appearance_token_income_description
    ColorToken.Expense -> R.string.appearance_token_expense_description
    ColorToken.Transfer -> R.string.appearance_token_transfer_description
    ColorToken.Upcoming -> R.string.appearance_token_upcoming_description
    ColorToken.Overdue -> R.string.appearance_token_overdue_description
    ColorToken.BudgetProgress -> R.string.appearance_token_budget_description
    ColorToken.SavingsProgress -> R.string.appearance_token_savings_description
}

@StringRes
internal fun ColorGroup.nameRes(): Int = when (this) {
    ColorGroup.Surfaces -> R.string.appearance_group_surfaces
    ColorGroup.Text -> R.string.appearance_group_text
    ColorGroup.Accents -> R.string.appearance_group_accents
    ColorGroup.Navigation -> R.string.appearance_group_navigation
    ColorGroup.Money -> R.string.appearance_group_money
    ColorGroup.Progress -> R.string.appearance_group_progress
}

@StringRes
internal fun ColorGroup.descriptionRes(): Int = when (this) {
    ColorGroup.Surfaces -> R.string.appearance_group_surfaces_description
    ColorGroup.Text -> R.string.appearance_group_text_description
    ColorGroup.Accents -> R.string.appearance_group_accents_description
    ColorGroup.Navigation -> R.string.appearance_group_navigation_description
    ColorGroup.Money -> R.string.appearance_group_money_description
    ColorGroup.Progress -> R.string.appearance_group_progress_description
}

@StringRes
internal fun AppearancePreset.nameRes(): Int = when (this) {
    AppearancePreset.Fern -> R.string.appearance_preset_fern
    AppearancePreset.Ivy -> R.string.appearance_preset_ivy
    AppearancePreset.Midnight -> R.string.appearance_preset_midnight
    AppearancePreset.Ember -> R.string.appearance_preset_ember
}
