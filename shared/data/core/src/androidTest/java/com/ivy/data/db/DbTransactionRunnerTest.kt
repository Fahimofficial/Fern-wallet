package com.ivy.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ivy.data.db.entity.CreditCardStatementEntity
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class DbTransactionRunnerTest {
    private lateinit var db: IvyRoomDatabase
    private lateinit var runner: DbTransactionRunner
    private val cardId = UUID.randomUUID()
    private val saved = CreditCardStatementEntity(
        accountId = cardId,
        nominalClosingEpochDay = 20_468,
        issuerStatementBalance = 1234.0,
    )

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, IvyRoomDatabase::class.java).build()
        runner = RoomDbTransactionRunner(db)
        runBlocking { db.writeCreditCardDao.saveStatement(saved) }
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun interruptedWrite_rollsBackEverything(): Unit = runBlocking {
        runCatching {
            runner.run {
                // Same shape as moving statement values: delete, then write (never reached).
                withContext(Dispatchers.IO) { db.writeCreditCardDao.deleteStatementsByAccountId(cardId) }
                error("app stopped part-way")
            }
        }

        db.creditCardDao.findStatementsByAccountId(cardId) shouldBe listOf(saved)
    }

    @Test
    fun completedWrite_commits(): Unit = runBlocking {
        val moved = saved.copy(nominalClosingEpochDay = 20_471)
        runner.run {
            db.writeCreditCardDao.deleteStatementsByAccountId(cardId)
            db.writeCreditCardDao.saveStatements(listOf(moved))
        }

        db.creditCardDao.findStatementsByAccountId(cardId) shouldBe listOf(moved)
    }
}
