package com.ivy.data.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * What the Home menu remembers about its Help & Support / Support My Work cards.
 *
 * @property permanentSupportCard whether the always-on Support My Work card is wanted (Appearance switch).
 * @property helpDismissedIn app version code in which the one-time Help & Support card was dismissed.
 * @property supportDismissedIn app version code in which the one-time Support My Work card was dismissed.
 */
data class SupportCardsPrefs(
    val permanentSupportCard: Boolean,
    val helpDismissedIn: Int?,
    val supportDismissedIn: Int?,
)

/**
 * Persists [SupportCardsPrefs]. The two one-time cards store the version code they were dismissed
 * in, separately, so each comes back by itself after an app update and neither affects the other.
 */
class SupportCardsDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val prefs: Flow<SupportCardsPrefs> = dataStore.data.map {
        SupportCardsPrefs(
            permanentSupportCard = it[PermanentSupportCardKey] ?: true,
            helpDismissedIn = it[HelpDismissedInKey],
            supportDismissedIn = it[SupportDismissedInKey],
        )
    }

    suspend fun setPermanentSupportCard(enabled: Boolean) {
        dataStore.edit { it[PermanentSupportCardKey] = enabled }
    }

    suspend fun dismissHelpCard(versionCode: Int) {
        dataStore.edit { it[HelpDismissedInKey] = versionCode }
    }

    suspend fun dismissSupportCard(versionCode: Int) {
        dataStore.edit { it[SupportDismissedInKey] = versionCode }
    }

    companion object {
        private val PermanentSupportCardKey = booleanPreferencesKey("home_support_card_enabled")
        private val HelpDismissedInKey = intPreferencesKey("home_help_card_dismissed_version")
        private val SupportDismissedInKey = intPreferencesKey("home_support_card_dismissed_version")
    }
}
