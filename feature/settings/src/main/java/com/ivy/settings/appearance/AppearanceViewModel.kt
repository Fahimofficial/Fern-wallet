package com.ivy.settings.appearance

import androidx.lifecycle.ViewModel
import com.ivy.base.di.AppCoroutineScope
import com.ivy.base.legacy.Theme
import com.ivy.data.datasource.AppearanceDataSource
import com.ivy.design.system.appearance.AppearanceSettings
import com.ivy.design.system.appearance.encode
import com.ivy.legacy.IvyWalletCtx
import com.ivy.legacy.domain.action.settings.UpdateSettingsAct
import com.ivy.wallet.domain.action.settings.SettingsAct
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    @AppCoroutineScope private val applicationScope: CoroutineScope,
) : ViewModel() {
    private val saveLock = Mutex()

    /** Snapshot state: reading it in composition recomposes on change. */
    val appearance: AppearanceSettings get() = ivyContext.appearance
    val theme: Theme get() = ivyContext.theme

    /** null until the first change on this screen; then whether a save is still running. */
    var saving: Boolean? by mutableStateOf(null)
        private set
    private var pendingSaves = 0

    fun update(transform: (AppearanceSettings) -> AppearanceSettings) {
        ivyContext.appearance = transform(ivyContext.appearance)
        pendingSaves++
        saving = true
        applicationScope.launch {
            // Encodes inside the lock, so the last write always holds the latest choice.
            saveLock.withLock { appearanceDataSource.set(ivyContext.appearance.encode()) }
            withContext(Dispatchers.Main) {
                pendingSaves--
                if (pendingSaves == 0) saving = false
            }
        }
    }

    fun setTheme(theme: Theme) {
        if (theme == ivyContext.theme) return
        ivyContext.switchTheme(theme)
        applicationScope.launch {
            updateSettingsAct(settingsAct.getSettings().copy(theme = theme))
        }
    }
}
