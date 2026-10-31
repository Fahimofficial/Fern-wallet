package com.ivy.settings

import com.ivy.data.backup.auto.AutoBackupFrequency

import com.ivy.domain.RootScreen

sealed interface SettingsEvent {
    data class SetCurrency(val newCurrency: String) : SettingsEvent
    data class SetName(val newName: String) : SettingsEvent
    data class ExportToCsv(val rootScreen: RootScreen) : SettingsEvent

    /** [password]: null for an unencrypted backup. */
    data class BackupData(val rootScreen: RootScreen, val password: String?) : SettingsEvent
    data class SetAutoBackupFolder(val treeUri: String) : SettingsEvent
    data class SetAutoBackupFrequency(val frequency: AutoBackupFrequency) : SettingsEvent
    data class SetAutoBackupPassword(val password: String?) : SettingsEvent
    data object BackUpNow : SettingsEvent
    data object SwitchTheme : SettingsEvent
    data class SetLockApp(val lockApp: Boolean) : SettingsEvent
    data class SetShowNotifications(val showNotifications: Boolean) : SettingsEvent
    data class SetHideCurrentBalance(val hideCurrentBalance: Boolean) : SettingsEvent
    data class SetHideIncome(val hideIncome: Boolean) : SettingsEvent
    data class SetTransfersAsIncomeExpense(val treatTransfersAsIncomeExpense: Boolean) :
        SettingsEvent

    data class SetStartDateOfMonth(val startDate: Int) : SettingsEvent
    data object DeleteCloudUserData : SettingsEvent
    data object DeleteAllUserData : SettingsEvent
    data object SwitchLanguage : SettingsEvent
}
