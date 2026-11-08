package com.ivy.data.repository

import com.ivy.data.media.AccountMediaFiles
import com.ivy.data.repository.mapper.appearance
import com.ivy.base.threading.DispatchersProvider
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.read.AccountDao
import com.ivy.data.db.dao.write.WriteAccountDao
import com.ivy.data.model.Account
import com.ivy.data.model.AccountId
import com.ivy.data.repository.mapper.AccountMapper
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(
    private val mapper: AccountMapper,
    private val accountDao: AccountDao,
    private val writeAccountDao: WriteAccountDao,
    private val dispatchersProvider: DispatchersProvider,
    memoFactory: RepositoryMemoFactory,
    private val mediaFiles: AccountMediaFiles,
) {
    private val memo = memoFactory.createMemo(
        getDataWriteSaveEvent = DataWriteEvent::SaveAccounts,
        getDateWriteDeleteEvent = DataWriteEvent::DeleteAccounts
    )

    suspend fun findById(id: AccountId): Account? = memo.findById(
        id = id,
        findByIdOperation = {
            accountDao.findById(id.value)?.let {
                with(mapper) { it.toDomain() }.getOrNull()
            }
        }
    )

    suspend fun findAll(): List<Account> = memo.findAll(
        findAllOperation = {
            accountDao.findAll().mapNotNull {
                with(mapper) { it.toDomain() }.getOrNull()
            }
        },
        sortMemo = { sortedBy(Account::orderNum) }
    )

    suspend fun findMaxOrderNum(): Double = if (memo.findAllMemoized) {
        memo.items.maxOfOrNull { (_, acc) -> acc.orderNum } ?: 0.0
    } else {
        withContext(dispatchersProvider.io) {
            accountDao.findMaxOrderNum() ?: 0.0
        }
    }

    suspend fun save(value: Account) {
        val replaced = mediaReplacedBy(value)
        memo.save(value) {
            writeAccountDao.save(
                with(mapper) { it.toEntity() }
            )
        }
        // After the write: a failed save must not lose the image the account still points at.
        mediaFiles.delete(replaced)
    }

    suspend fun saveMany(values: List<Account>): Unit = memo.saveMany(values) {
        writeAccountDao.saveMany(
            it.map { with(mapper) { it.toEntity() } }
        )
    }

    suspend fun deleteById(id: AccountId) {
        val files = withContext(dispatchersProvider.io) { accountDao.findById(id.value)?.appearance()?.files }
        memo.deleteById(id) {
            writeAccountDao.deleteById(id.value)
        }
        mediaFiles.delete(files.orEmpty())
    }

    suspend fun deleteAll() {
        memo.deleteAll(
            deleteAllOperation = writeAccountDao::deleteAll
        )
        mediaFiles.deleteAll()
    }

    // Logo and background files the stored account uses that [value] no longer does.
    private suspend fun mediaReplacedBy(value: Account): List<String> = withContext(dispatchersProvider.io) {
        val before = accountDao.findById(value.id.value)?.appearance()?.files.orEmpty()
        before - value.appearance.files.toSet()
    }
}
