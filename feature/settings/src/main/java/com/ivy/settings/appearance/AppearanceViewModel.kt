package com.ivy.settings.appearance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.base.legacy.Theme
import com.ivy.data.datasource.AppearanceDataSource
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.encode
import com.ivy.legacy.IvyWalletCtx
import com.ivy.wallet.domain.action.settings.SettingsAct
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Edits the app-wide appearance. Changes go to [IvyWalletCtx] first, so every screen (and the
 * preview) recolours immediately, then they are saved.
 */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val ivyContext: IvyWalletCtx,
    private val appearanceDataSource: AppearanceDataSource,
    private val settingsAct: SettingsAct,
    private val updateSettingsAct: UpdateSettingsAct,
) : ViewModel() {
    private val saveLock = Mutex()

    /** Snapshot state: reading it in composition recomposes on change. */
    val appearance: AppearanceSettings get() = ivyContext.appearance
    val theme: Theme get() = ivyContext.theme

    fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        ivyContext.appearance = transform(ivyContext.appearance)
        viewModelScope.launch {
            // Leaving the screen right after a tap must not lose the change.
            withContext(NonCancellable) {
                // Encodes inside the lock, so the last write always holds the latest choice.
                saveLock.withLock { appearanceDataSource.set(ivyContext.appearance.encode()) }
            }
        }
    }

    fun setTheme(theme: Theme) {
        if (theme == ivyContext.theme) return
        ivyContext.switchTheme(theme)
        viewModelScope.launch {
            withContext(NonCancellable) {
                updateSettingsAct(settingsAct.getSettings().copy(theme = theme))
            }
        }
    }
}
