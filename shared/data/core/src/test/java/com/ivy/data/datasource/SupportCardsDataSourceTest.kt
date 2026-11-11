package com.ivy.data.datasource

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class SupportCardsDataSourceTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
    private val file by lazy { File(folder.root, "support_cards.preferences_pb") }

    /** Defaults of a fresh install; [SupportCardsPrefs] itself has none. */
    private fun prefs(
        permanentSupportCard: Boolean = true,
        helpDismissedFor: Long? = null,
        supportDismissedFor: Long? = null,
    ) = SupportCardsPrefs(permanentSupportCard, helpDismissedFor, supportDismissedFor)

    private fun dataSource() = SupportCardsDataSource(
        PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
    )

    @After
    fun tearDown() = scope.cancel()

    @Test
    fun `defaults on a fresh install`() = runTest {
        dataSource().prefs.first() shouldBe prefs(
            permanentSupportCard = true,
            helpDismissedFor = null,
            supportDismissedFor = null,
        )
    }

    @Test
    fun `the two one-time cards are dismissed independently`() = runTest {
        val dataSource = dataSource()

        dataSource.dismissHelpCard(1_000L)
        dataSource.prefs.first() shouldBe prefs(helpDismissedFor = 1_000L, supportDismissedFor = null)

        dataSource.dismissSupportCard(2_000L)
        dataSource.prefs.first() shouldBe prefs(helpDismissedFor = 1_000L, supportDismissedFor = 2_000L)
    }

    @Test
    fun `the permanent card switch does not touch the dismissals`() = runTest {
        val dataSource = dataSource()
        dataSource.dismissHelpCard(1_000L)
        dataSource.dismissSupportCard(1_000L)

        dataSource.setPermanentSupportCard(false)

        dataSource.prefs.first() shouldBe prefs(
            permanentSupportCard = false,
            helpDismissedFor = 1_000L,
            supportDismissedFor = 1_000L,
        )
    }

    @Test
    fun `everything survives a restart`() = runTest {
        val firstRun = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        SupportCardsDataSource(PreferenceDataStoreFactory.create(scope = firstRun, produceFile = { file })).apply {
            setPermanentSupportCard(false)
            dismissHelpCard(1_000L)
            dismissSupportCard(500L)
        }
        firstRun.cancel() // the app process ends; a file allows only one open DataStore

        val secondRun = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())
        val restarted = SupportCardsDataSource(
            PreferenceDataStoreFactory.create(scope = secondRun, produceFile = { file })
        )

        restarted.prefs.first() shouldBe prefs(
            permanentSupportCard = false,
            helpDismissedFor = 1_000L,
            supportDismissedFor = 500L,
        )
        secondRun.cancel()
    }
}
