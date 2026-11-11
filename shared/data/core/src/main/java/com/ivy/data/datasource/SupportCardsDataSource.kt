package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * What the Home menu remembers about its Help & Support / Support My Work cards.
 *
 * An install stamp is the time the app was last installed or updated (see `PackageInfo.lastUpdateTime`).
 * It changes with every fresh install and every update, even one that keeps the same version code.
 *
 * @property permanentSupportCard whether the always-on Support My Work card is wanted (Appearance switch).
 * @property helpDismissedFor install stamp for which the one-time Help & Support card was dismissed.
 * @property supportDismissedFor install stamp for which the one-time Support My Work card was dismissed.
 */
data class SupportCardsPrefs(
    val permanentSupportCard: Boolean,
    val helpDismissedFor: Long?,
    val supportDismissedFor: Long?,
)

/**
 * Persists [SupportCardsPrefs]. The two one-time cards store the install stamp they were dismissed
 * for, separately, so each comes back by itself after the next install or update and neither
 * affects the other.
 */
class SupportCardsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val prefs: Flow<SupportCardsPrefs> = dataStore.data.map {
        SupportCardsPrefs(
            permanentSupportCard = it[PermanentSupportCardKey] ?: true,
            helpDismissedFor = it[HelpDismissedForKey],
            supportDismissedFor = it[SupportDismissedForKey],
        )
    }

    suspend fun setPermanentSupportCard(enabled: Boolean) {
        dataStore.edit { it[PermanentSupportCardKey] = enabled }
    }

    suspend fun dismissHelpCard(installStamp: Long) {
        dataStore.edit { it[HelpDismissedForKey] = installStamp }
    }

    suspend fun dismissSupportCard(installStamp: Long) {
        dataStore.edit { it[SupportDismissedForKey] = installStamp }
    }

    companion object {
        private val PermanentSupportCardKey = booleanPreferencesKey("home_support_card_enabled")

        // New key names: the version-code ints saved by the first release of these cards are ignored,
        // so the cards show once more after this update.
        private val HelpDismissedForKey = longPreferencesKey("home_help_card_dismissed_install")
        private val SupportDismissedForKey = longPreferencesKey("home_support_card_dismissed_install")
    }
}
