package com.ivy.data.backup

import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.base.legacy.SharedPrefs
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
import com.ivy.data.repository.AccountRepository
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import com.ivy.data.testResource
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class BackupDataUseCaseTest {
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
            accountRepository = AccountRepository(
                accountDao = accountDao,
                writeAccountDao = accountDao,
                mapper = accountMapper,
                dispatchersProvider = TestDispatchersProvider,
                memoFactory = fakeRepositoryMemoFactory(),
            ),
            budgetDao = budgetDao,
            categoryDao = categoryDao,
            loanRecordDao = loanRecordDao,
            loanDao = loanDao,
            plannedPaymentRuleDao = plannedPaymentDao,
            transactionDao = transactionDao,
            transactionWriter = transactionDao,
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
            fileSystem = mockk(relaxed = true),
            dataObserver = DataObserver(),
            tagsReader = tagDao,
            tagsWriter = tagDao,
            tagAssociationReader = tagAssociationDao,
            tagAssociationWriter = tagAssociationDao,
            creditCardDao = creditCardDao,
            creditCardWriter = creditCardDao,
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
        val backupJsonData = testResource("backups/450-150.json").readText(Charsets.UTF_16)

        // when
        useCase.importJson(backupJsonData, onProgress = {})

        // then
        verify { sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, true) }
        verify(exactly = 0) { sharedPrefs.putBoolean(SharedPrefs.APP_LOCK_ENABLED, false) }
    }

    @Test
    fun `old backups without credit cards still import as asset accounts`() = runTest {
        val accountDao = FakeAccountDao()
        val creditCardDao = FakeCreditCardDao()
        val useCase = newBackupDataUseCase(accountDao = accountDao, creditCardDao = creditCardDao)
        val backupJson = testResource("backups/450-150.json").readText(Charsets.UTF_16)
        backupJson.contains("creditCards") shouldBe false

        useCase.importJson(backupJson, onProgress = {})

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
}
