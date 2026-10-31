package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.base.legacy.SharedPrefs
import com.ivy.base.legacy.Theme
import com.ivy.data.DataObserver
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeBudgetDao
import com.ivy.data.db.dao.fake.FakeCategoryDao
import com.ivy.data.db.dao.fake.FakeCreditCardDao
import com.ivy.data.db.dao.fake.FakeLoanDao
import com.ivy.data.db.dao.fake.FakeLoanRecordDao
import com.ivy.data.db.dao.fake.FakePlannedPaymentDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.dao.fake.FakeTagAssociationDao
import com.ivy.data.db.dao.fake.FakeTagDao
import com.ivy.data.db.dao.fake.FakeTransactionDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.db.entity.AccountTypeAsset
import com.ivy.data.db.entity.AccountTypeCreditCard
import com.ivy.data.db.entity.CreditCardEntity
import com.ivy.data.db.entity.CreditCardStatementEntity
import com.ivy.data.db.entity.SettingsEntity
import com.ivy.data.db.entity.TagEntity
import com.ivy.data.db.entity.TransactionEntity
import com.ivy.data.db.DbTransactionRunner
import com.ivy.data.db.dao.fake.FakeDbTransactionRunner
import com.ivy.data.db.dao.write.WriteTransactionDao
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.testResource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.Instant
import java.util.UUID

class BackupDataUseCaseTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun newBackupDataUseCase(
        accountDao: FakeAccountDao = FakeAccountDao(),
        categoryDao: FakeCategoryDao = FakeCategoryDao(),
        transactionDao: FakeTransactionDao = FakeTransactionDao(),
        plannedPaymentDao: FakePlannedPaymentDao = FakePlannedPaymentDao(),
        budgetDao: FakeBudgetDao = FakeBudgetDao(),
        settingsDao: FakeSettingsDao = FakeSettingsDao(),
        loanDao: FakeLoanDao = FakeLoanDao(),
        loanRecordDao: FakeLoanRecordDao = FakeLoanRecordDao(),
        tagDao: FakeTagDao = FakeTagDao(),
        tagAssociationDao: FakeTagAssociationDao = FakeTagAssociationDao(),
        sharedPrefs: SharedPrefs = mockk(relaxed = true),
        creditCardDao: FakeCreditCardDao = FakeCreditCardDao(),
        transactionWriter: WriteTransactionDao = transactionDao,
        transactionRunner: DbTransactionRunner = FakeDbTransactionRunner(),
        snapshots: PreRestoreSnapshots = PreRestoreSnapshots { tempFolder.root.resolve("pre-restore") },
    ): BackupDataUseCase {
        val accountMapper = AccountMapper(
            CurrencyRepository(
                settingsDao = settingsDao,
                writeSettingsDao = settingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = accountDao,
            accountMapper = accountMapper,
            accountWriter = accountDao,
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionWriter,
            settingsDao = settingsDao,
            categoryWriter = categoryDao,
            settingsWriter = settingsDao,
            budgetWriter = budgetDao,
            loanWriter = loanDao,
            loanRecordWriter = loanRecordDao,
            plannedPaymentRuleWriter = plannedPaymentDao,

            context = mockk(relaxed = true),
            sharedPrefs = sharedPrefs,
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            creditCardDao = creditCardDao,
            creditCardWriter = creditCardDao,
            transactionRunner = transactionRunner,
            preRestoreSnapshots = snapshots,
        )
    }

    private suspend fun backupTestCase(backupVersion: String) {
        // given
        val originalBackupUseCase = newBackupDataUseCase()
        val backupJsonData = testResource("backups/$backupVersion.json")
            .readText(Charsets.UTF_16)

        // when
        val importedDataRes = originalBackupUseCase.importJson(backupJsonData, onProgress = {})

        // then
        importedDataRes.accountsImported shouldBeGreaterThan 0
        importedDataRes.transactionsImported shouldBeGreaterThan 0
        importedDataRes.categoriesImported shouldBeGreaterThan 0
        importedDataRes.failedRows.size shouldBe 0

        // Also - exporting and re-importing the data should work
        // given
        val exportedJson = originalBackupUseCase.generateJsonBackup()

        // when
        val freshBackupUseCase = newBackupDataUseCase()
        val reImportedDataRes = freshBackupUseCase.importJson(exportedJson, onProgress = {})
        // then
        reImportedDataRes shouldBe importedDataRes

        // Finally, exporting again should yield the same result
        freshBackupUseCase.generateJsonBackup() shouldBe exportedJson
    }

    @Test
    fun `backup compatibility with 450 (150)`() = runTest {
        backupTestCase("450-150")
    }

    @Test
    fun `importing a backup never disables an enabled app lock`() = runTest {
        // given: app lock is enabled on this device; the backup has it disabled
        val sharedPrefs = mockk<SharedPrefs>(relaxed = true)
        every { sharedPrefs.getBoolean(SharedPrefs.APP_LOCK_ENABLED, any()) } returns true
        val useCase = newBackupDataUseCase(sharedPrefs = sharedPrefs)
        // when
        useCase.importJson(backupJson(), onProgress = {})

        // then
        verify { sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, true) }
        verify(exactly = 0) { sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, false) }
    }

    @Test
    fun `old backups without credit cards still import as asset accounts`() = runTest {
        val accountDao = FakeAccountDao()
        val creditCardDao = FakeCreditCardDao()
        val useCase = newBackupDataUseCase(accountDao = accountDao, creditCardDao = creditCardDao)
        val json = backupJson()
        json.contains("creditCards") shouldBe false

        useCase.importJson(json, onProgress = {})

        accountDao.findAll().shouldNotBeEmpty()
        accountDao.findAll().all { it.type == AccountTypeAsset } shouldBe true
        creditCardDao.findAll().shouldBeEmpty()
    }

    @Test
    fun `credit cards and statement overrides survive a backup round trip`() = runTest {
        val accountDao = FakeAccountDao()
        val creditCardDao = FakeCreditCardDao()
        val cardId = UUID.fromString("00000000-0000-0000-0000-00000000c0de")
        accountDao.save(
            AccountEntity(
                name = "My card",
                currency = "INR",
                color = 0,
                type = AccountTypeCreditCard,
                id = cardId,
            )
        )
        val card = CreditCardEntity(
            accountId = cardId,
            bankId = "hdfc-bank",
            productName = "Custom product",
            network = "RUPAY",
            lastFour = "4321",
            creditLimit = 150000.0,
            statementDay = 31,
            dueRuleType = "DAYS_AFTER_STATEMENT",
            dueRuleValue = 18,
            openingOutstanding = 1234.56,
            openingDateEpochDay = 20_000,
            status = "CLOSED",
            appearance = "OCEAN",
            remindersEnabled = false,
            reminderDaysBefore = 5,
        )
        val statement = CreditCardStatementEntity(
            accountId = cardId,
            nominalClosingEpochDay = 20_030,
            actualDueEpochDay = 20_050,
            issuerStatementBalance = 999.99,
            minimumDue = 100.0,
        )
        creditCardDao.save(card)
        creditCardDao.saveStatement(statement)
        val exported = newBackupDataUseCase(accountDao = accountDao, creditCardDao = creditCardDao)
            .generateJsonBackup()

        val freshAccounts = FakeAccountDao()
        val freshCards = FakeCreditCardDao()
        newBackupDataUseCase(accountDao = freshAccounts, creditCardDao = freshCards)
            .importJson(exported, onProgress = {})

        freshAccounts.findById(cardId)!!.type shouldBe AccountTypeCreditCard
        freshCards.findAll() shouldBe listOf(card)
        freshCards.findAllStatements() shouldBe listOf(statement)
    }

    private fun backupJson(): String = testResource("backups/450-150.json").readText(Charsets.UTF_16)

    @Test
    fun `exported backups declare the current format version`() = runTest {
        val useCase = newBackupDataUseCase()
        useCase.importJson(backupJson())

        useCase.generateJsonBackup() shouldContain
            "\"formatVersion\":${IvyWalletCompleteData.CURRENT_FORMAT_VERSION}"
    }

    @Test
    fun `a corrupted backup reports CORRUPTED and changes nothing`() = runTest {
        val accountDao = FakeAccountDao()
        val sharedPrefs = mockk<SharedPrefs>(relaxed = true)
        val useCase = newBackupDataUseCase(accountDao = accountDao, sharedPrefs = sharedPrefs)

        val result = useCase.importJson(backupJson().take(5_000))

        result.failure shouldBe ImportFailure.CORRUPTED
        result.transactionsImported shouldBe 0
        accountDao.findAll().shouldBeEmpty()
        verify(exactly = 0) { sharedPrefs.putBoolean(any(), any()) }
    }

    @Test
    fun `a backup from a newer format is refused`() = runTest {
        val accountDao = FakeAccountDao()
        val useCase = newBackupDataUseCase(accountDao = accountDao)
        val newer = backupJson().replaceFirst(
            "{",
            "{\"formatVersion\":${IvyWalletCompleteData.CURRENT_FORMAT_VERSION + 1},"
        )

        val result = useCase.importJson(newer)

        result.failure shouldBe ImportFailure.NEWER_VERSION
        accountDao.findAll().shouldBeEmpty()
    }

    @Test
    fun `all writes happen inside one database transaction`() = runTest {
        val runner = FakeDbTransactionRunner()
        val transactionDao = FakeTransactionDao()
        val writes = mutableListOf<Boolean>()
        val recordingWriter = object : WriteTransactionDao by transactionDao {
            override suspend fun saveMany(value: List<TransactionEntity>) {
                writes += runner.inTransaction
                transactionDao.saveMany(value)
            }
        }
        val useCase = newBackupDataUseCase(
            transactionDao = transactionDao,
            transactionWriter = recordingWriter,
            transactionRunner = runner,
        )

        useCase.importJson(backupJson()).failure shouldBe null

        writes shouldBe listOf(true)
    }

    @Test
    fun `a failure while saving reports SAVE_FAILED, keeps prefs and offers no undo`() = runTest {
        val accountDao = FakeAccountDao()
        accountDao.save(AccountEntity(name = "Existing", currency = "EUR", color = 0))
        val snapshots = PreRestoreSnapshots { tempFolder.root.resolve("pre-restore") }
        val sharedPrefs = mockk<SharedPrefs>(relaxed = true)
        val transactionDao = FakeTransactionDao()
        val failingWriter = object : WriteTransactionDao by transactionDao {
            override suspend fun saveMany(value: List<TransactionEntity>) {
                error("disk full")
            }
        }
        val useCase = newBackupDataUseCase(
            accountDao = accountDao,
            transactionDao = transactionDao,
            transactionWriter = failingWriter,
            sharedPrefs = sharedPrefs,
            snapshots = snapshots,
        )

        val result = useCase.importJson(backupJson())

        result.failure shouldBe ImportFailure.SAVE_FAILED
        result.canUndo shouldBe false
        snapshots.latest() shouldBe null
        verify(exactly = 0) { sharedPrefs.putBoolean(any(), any()) }
    }

    @Test
    fun `importing into a wallet with data can be undone exactly`() = runTest {
        // given: a wallet with its own data
        val accountDao = FakeAccountDao()
        val categoryDao = FakeCategoryDao()
        val transactionDao = FakeTransactionDao()
        val settingsDao = FakeSettingsDao()
        val useCase = newBackupDataUseCase(
            accountDao = accountDao,
            categoryDao = categoryDao,
            transactionDao = transactionDao,
            settingsDao = settingsDao,
        )
        accountDao.save(AccountEntity(name = "My own account", currency = "INR", color = 1))
        val before = useCase.generateJsonBackup()

        // when: a backup is imported
        val result = useCase.importJson(backupJson())

        // then: it can be undone, and undo brings back exactly the previous data
        result.failure shouldBe null
        result.canUndo shouldBe true
        transactionDao.findAll().shouldNotBeEmpty()

        useCase.undoLastImport() shouldBe true
        useCase.generateJsonBackup() shouldBe before
        useCase.canUndoLastImport() shouldBe false
    }

    @Test
    fun `a wallet with only tags (no accounts or categories) can be restored after an import`() = runTest {
        val tagDao = FakeTagDao()
        val useCase = newBackupDataUseCase(tagDao = tagDao)
        tagDao.save(
            TagEntity(
                id = UUID.randomUUID(),
                name = "Work",
                description = null,
                color = 0,
                icon = null,
                orderNum = 0.0,
                dateTime = Instant.EPOCH,
                isDeleted = false,
                lastSyncedTime = Instant.EPOCH,
            )
        )
        val before = useCase.generateJsonBackup()

        val result = useCase.importJson(backupJson())

        result.canUndo shouldBe true
        useCase.undoLastImport() shouldBe true
        useCase.generateJsonBackup() shouldBe before
    }

    @Test
    fun `importing into an empty wallet saves no copy`() = runTest {
        val useCase = newBackupDataUseCase()

        val result = useCase.importJson(backupJson())

        result.canUndo shouldBe false
        useCase.canUndoLastImport() shouldBe false
        useCase.undoLastImport() shouldBe false
    }

    @Test
    fun `a backup without settings keeps the current settings`() = runTest {
        val settingsDao = FakeSettingsDao()
        val current = SettingsEntity(theme = Theme.DARK, currency = "INR", bufferAmount = 0.0, name = "")
        settingsDao.save(current)
        val useCase = newBackupDataUseCase(settingsDao = settingsDao)

        useCase.importJson("{\"accounts\":[]}").failure shouldBe null

        settingsDao.findAll() shouldBe listOf(current)
    }
}
