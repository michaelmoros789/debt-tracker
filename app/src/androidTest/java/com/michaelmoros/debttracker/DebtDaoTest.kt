package com.michaelmoros.debttracker

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class DebtDaoTest {
    private lateinit var debtDao: DebtDao
    private lateinit var db: DebtDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(
            context, DebtDatabase::class.java
        ).build()
        debtDao = db.debtDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun writeDebtAndReadInList() = runBlocking {
        val debt = DebtEntity(name = "Test Person", context = "Work")
        val debtId = debtDao.insertDebt(debt)
        
        val allDebts = debtDao.getAllDebts().first()
        assertEquals(allDebts[0].debt.name, "Test Person")
        assertEquals(allDebts[0].debt.id, debtId)
    }

    @Test
    @Throws(Exception::class)
    fun balanceCalculationIsCorrect() = runBlocking {
        val debtId = debtDao.insertDebt(DebtEntity(name = "Finance Test", context = "Work"))
        
        // Minor units (e.g. 50.00)
        debtDao.insertTransaction(TransactionEntity(debtId = debtId, date = 1000L, description = "Lent", method = "Cash", amount = 5000))
        debtDao.insertTransaction(TransactionEntity(debtId = debtId, date = 2000L, description = "Borrowed", method = "Cash", amount = -2000))
        
        val debtWithBalance = debtDao.getDebtById(debtId).first()
        
        assertEquals(3000L, debtWithBalance?.balance)
        assertEquals(2, debtWithBalance?.transactionCount)
    }

    @Test
    @Throws(Exception::class)
    fun deletingTransactionUpdatesBalance() = runBlocking {
        val debtId = debtDao.insertDebt(DebtEntity(name = "Delete Test", context = "Work"))
        
        val tx1 = TransactionEntity(id = 101, debtId = debtId, date = 1000L, description = "T1", method = "Cash", amount = 1000)
        val tx2 = TransactionEntity(id = 102, debtId = debtId, date = 2000L, description = "T2", method = "Cash", amount = 2000)
        
        debtDao.insertTransaction(tx1)
        debtDao.insertTransaction(tx2)
        
        var balance = debtDao.getDebtById(debtId).first()?.balance
        assertEquals(3000L, balance)
        
        debtDao.deleteTransaction(tx1)
        
        balance = debtDao.getDebtById(debtId).first()?.balance
        assertEquals(2000L, balance)
    }
}
