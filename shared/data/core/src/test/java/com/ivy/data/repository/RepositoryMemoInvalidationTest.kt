package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.DataObserver
import com.ivy.data.DataWriteEvent
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.db.entity.AccountEntity
import com.ivy.data.repository.mapper.AccountMapper
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test

class RepositoryMemoInvalidationTest {
    @Test
    fun `cached accounts are dropped after an all-data change (like a backup import)`() = runTest {
        val dataObserver = DataObserver()
        val accountDao = FakeAccountDao()
        val settingsDao = FakeSettingsDao()
        val repository = AccountRepository(
            mapper = AccountMapper(CurrencyRepository(settingsDao, settingsDao, TestDispatchersProvider)),
            accountDao = accountDao,
            writeAccountDao = accountDao,
            dispatchersProvider = TestDispatchersProvider,
            memoFactory = RepositoryMemoFactory(dataObserver, TestDispatchersProvider),
        )
        accountDao.save(AccountEntity(name = "Cash", currency = "EUR", color = 0))
        repository.findAll().map { it.name.value } shouldBe listOf("Cash")

        // A bulk write that bypasses the repository, like a backup import
        accountDao.save(AccountEntity(name = "Bank", currency = "EUR", color = 0, orderNum = 1.0))
        repository.findAll().map { it.name.value } shouldBe listOf("Cash") // still cached

        dataObserver.post(DataWriteEvent.AllDataChange)

        repository.findAll().map { it.name.value } shouldBe listOf("Cash", "Bank")
    }
}
