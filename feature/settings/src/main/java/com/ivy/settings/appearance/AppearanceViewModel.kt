package com.ivy.settings.appearance

import android.content.Context
import com.ivy.domain.features.Features
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ivy.base.di.AppCoroutineScope
import com.ivy.base.legacy.Theme
import com.ivy.data.datasource.AppearanceDataSource
import com.ivy.data.datasource.SupportCardsDataSource
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
    private val features: Features,
    @ApplicationContext private val appContext: Context,
    private val supportCardsDataSource: SupportCardsDataSource,
) : ViewModel() {
    private val saveLock = Mutex()

    /** Snapshot state: reading it in composition recomposes on change. */
    val appearance: AppearanceSettings get() = ivyContext.appearance
    val theme: Theme get() = ivyContext.theme

    /** null until the first change on this screen; then whether a save is still running. */
    var saving: Boolean? by mutableStateOf(null)
        private set
    private var pendingSaves = 0

    /**
     * "Show Support My Work card": the permanent card in the Home menu. Follows the stored value;
     * it doesn't touch the one-time cards shown after an install or update.
     */
    var showSupportCard: Boolean by mutableStateOf(true)
        private set

    init {
        viewModelScope.launch { supportCardsDataSource.prefs.collect { showSupportCard = it.permanentSupportCard } }
    }

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

    /**
     * "Use account colours on transactions": the existing feature flag, so Settings > Features
     * and this switch stay one setting. The app root collects it; every list updates at once.
     */
    fun setAccountColorsInTransactions(enabled: Boolean) {
        applicationScope.launch { features.showAccountColorsInTransactions.set(appContext, enabled) }
    }

    fun setSupportCardEnabled(enabled: Boolean) {
        applicationScope.launch { supportCardsDataSource.setPermanentSupportCard(enabled) }
    }

    fun setTheme(theme: Theme) {
        if (theme == ivyContext.theme) return
        ivyContext.switchTheme(theme)
        applicationScope.launch {
            updateSettingsAct(settingsAct.getSettings().copy(theme = theme))
        }
    }
}
