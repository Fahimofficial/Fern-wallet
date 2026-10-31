package com.ivy.data.backup

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ivy.base.TestDispatchersProvider
import com.ivy.base.di.KotlinxSerializationModule
import com.ivy.base.legacy.SharedPrefs
import com.ivy.data.DataObserver
import com.ivy.data.db.IvyRoomDatabase
import com.ivy.data.db.RoomDbTransactionRunner
import com.ivy.data.db.dao.write.WriteLoanDao
import com.ivy.data.db.entity.LoanEntity
import com.ivy.data.repository.CurrencyRepository
import com.ivy.data.repository.mapper.AccountMapper
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupDataUseCaseAndroidTest {

    private lateinit var db: IvyRoomDatabase
    private lateinit var useCase: BackupDataUseCase

    @Before
    fun createDb() {
        InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
            .resolve("test-pre-restore").deleteRecursively()
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()
        useCase = newUseCase()
    }

    private fun newUseCase(
        loanWriter: WriteLoanDao = db.writeLoanDao,
    ): BackupDataUseCase {
        val appContext = InstrumentationRegistry.getInstrumentation().context
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val snapshots = File(targetContext.cacheDir, "test-pre-restore").also { snapshotDir = it }
        val accountMapper = AccountMapper(
            currencyRepository = CurrencyRepository(
                settingsDao = db.settingsDao,
                writeSettingsDao = db.writeSettingsDao,
                dispatchersProvider = TestDispatchersProvider,
            )
        )
        return BackupDataUseCase(
            accountDao = db.accountDao,
            budgetDao = db.budgetDao,
            categoryDao = db.categoryDao,
            loanRecordDao = db.loanRecordDao,
            loanDao = db.loanDao,
            plannedPaymentRuleDao = db.plannedPaymentRuleDao,
            settingsDao = db.settingsDao,
            transactionDao = db.transactionDao,
            transactionWriter = db.writeTransactionDao,
            sharedPrefs = SharedPrefs(appContext),
            accountWriter = db.writeAccountDao,
            accountMapper = accountMapper,
            categoryWriter = db.writeCategoryDao,
            settingsWriter = db.writeSettingsDao,
            budgetWriter = db.writeBudgetDao,
            loanWriter = loanWriter,
            loanRecordWriter = db.writeLoanRecordDao,
            plannedPaymentRuleWriter = db.writePlannedPaymentRuleDao,
            context = targetContext,
            json = KotlinxSerializationModule.provideJson(),
            dispatchersProvider = TestDispatchersProvider,
            dataObserver = DataObserver(),
            tagsReader = db.tagDao,
            tagAssociationReader = db.tagAssociationDao,
            tagsWriter = db.writeTagDao,
            tagAssociationWriter = db.writeTagAssociationDao,
            creditCardDao = db.creditCardDao,
            creditCardWriter = db.writeCreditCardDao,
            transactionRunner = RoomDbTransactionRunner(db),
            preRestoreSnapshots = PreRestoreSnapshots { snapshots },
        )
    }

    private var snapshotDir: File? = null

    @After
    fun closeDb() {
        db.close()
        snapshotDir?.deleteRecursively()
    }

    @Test
    fun aFailureWhileSavingRollsBackEveryWrite() = runBlocking<Unit> {
        // given: loans are written after transactions, accounts and categories
        val failingLoans = object : WriteLoanDao by db.writeLoanDao {
            override suspend fun saveMany(value: List<LoanEntity>) {
                error("Simulated failure")
            }
        }
        val failingUseCase = newUseCase(loanWriter = failingLoans)
        val json = InstrumentationRegistry.getInstrumentation().targetContext.assets
            .open("backups/450-150.json").use { BackupArchive.readJson(it) }

        // when
        val res = failingUseCase.importJson(json)

        // then: nothing was written
        res.failure shouldBe ImportFailure.SAVE_FAILED
        db.transactionDao.findAll().shouldBeEmpty()
        db.accountDao.findAll().shouldBeEmpty()
        db.categoryDao.findAll().shouldBeEmpty()
    }

    @Test
    fun undoRestoresTheWalletAsItWasBeforeTheImport() = runBlocking<Unit> {
        // given: a wallet with data
        useCase.importBackupFile(copyTestResourceToInternalStorage("backups/450-150.zip"), onProgress = {})
            .shouldBeSuccessful()
        val before = useCase.generateJsonBackup()
        val exported = tempAndroidFile("exported", ".zip").toUri()
        useCase.exportToFile(exported)

        // when: a backup is imported on top, then undone
        val res = useCase.importBackupFile(exported, onProgress = {})
        res.shouldBeSuccessful()
        res.canUndo shouldBe true

        // then
        useCase.undoLastImport() shouldBe true
        useCase.generateJsonBackup() shouldBe before
    }

    @Test
    fun backup450_150() = runBlocking {
        backupTestCase("450-150")
    }

    private suspend fun backupTestCase(version: String) {
        importBackupZipTestCase(version)
        importBackupJsonTestCase(version)

        // close and re-open the db to ensure fresh data
        closeDb()
        createDb()
        exportsAndImportsTestCase(version)
    }

    private suspend fun importBackupZipTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun importBackupJsonTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.json")

        // when
        val res = useCase.importBackupFile(backupUri, onProgress = {})

        // then
        res.shouldBeSuccessful()
    }

    private suspend fun exportsAndImportsTestCase(version: String) {
        // given
        val backupUri = copyTestResourceToInternalStorage("backups/$version.zip")
        // preload data
        useCase.importBackupFile(backupUri, onProgress = {}).shouldBeSuccessful()
        val exportedFileUri = tempAndroidFile("exported", ".zip").toUri()

        // then
        useCase.exportToFile(exportedFileUri)
        useCase.importBackupFile(backupUri, onProgress = {}).shouldBeSuccessful()
        useCase.importBackupFile(exportedFileUri, onProgress = {}).shouldBeSuccessful()
    }

    private fun copyTestResourceToInternalStorage(resPath: String): Uri {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val assetManager = context.assets
        val inputStream = assetManager.open(resPath)
        val outputFile = tempAndroidFile("temp-backup", resPath.split(".").last())
        outputFile.outputStream().use { fileOut ->
            fileOut.write(inputStream.readBytes())
        }
        return Uri.fromFile(outputFile)
    }

    private fun tempAndroidFile(prefix: String, suffix: String): File {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        return File.createTempFile(prefix, suffix, context.filesDir)
    }

    private fun ImportResult.shouldBeSuccessful() {
        failure shouldBe null
        failedRows.shouldBeEmpty()
        categoriesImported shouldBeGreaterThan 0
        accountsImported shouldBeGreaterThan 0
        transactionsImported shouldBeGreaterThan 0
    }
}
