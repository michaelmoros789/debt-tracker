package com.michaelmoros.debttracker

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val context: String
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = DebtEntity::class,
            parentColumns = ["id"],
            childColumns = ["debtId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("debtId")]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val date: Long,
    val description: String,
    val method: String,
    val amount: Long, // Stored in minor units (e.g., cents/centavos)
    val referenceNumber: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * A data class that represents a Debt along with its calculated balance, transaction count, and last activity date.
 */
data class DebtWithBalance(
    @Embedded val debt: DebtEntity,
    val balance: Long, // Balance in minor units
    val transactionCount: Int,
    val lastTransactionDate: Long?
)

@Entity(tableName = "contexts")
data class ContextEntity(
    @PrimaryKey val name: String,
    val isHidden: Boolean = false
)

data class DebtWithTransactions(
    @Embedded val debt: DebtEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "debtId"
    )
    val transactions: List<TransactionEntity>
)

@Dao
interface DebtDao {
    @Query("""
        SELECT *, 
        (SELECT SUM(amount) FROM transactions WHERE debtId = debts.id) as balance,
        (SELECT COUNT(*) FROM transactions WHERE debtId = debts.id) as transactionCount,
        (SELECT MAX(date) FROM transactions WHERE debtId = debts.id) as lastTransactionDate
        FROM debts
    """)
    fun getAllDebts(): Flow<List<DebtWithBalance>>

    @Query("""
        SELECT *, 
        (SELECT SUM(amount) FROM transactions WHERE debtId = debts.id) as balance,
        (SELECT COUNT(*) FROM transactions WHERE debtId = debts.id) as transactionCount,
        (SELECT MAX(date) FROM transactions WHERE debtId = debts.id) as lastTransactionDate
        FROM debts WHERE id = :id
    """)
    fun getDebtById(id: Long): Flow<DebtWithBalance?>

    @Query("SELECT * FROM transactions WHERE debtId = :personId ORDER BY date DESC, createdAt DESC")
    fun getTransactionsForPerson(personId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Transaction
    @Query("SELECT * FROM debts WHERE id = :debtId")
    fun getDebtWithTransactions(debtId: Long): Flow<DebtWithTransactions?>

    @Transaction
    @Query("SELECT * FROM debts")
    fun getAllDebtsWithTransactions(): Flow<List<DebtWithTransactions>>

    @Query("SELECT * FROM debts LIMIT 1")
    suspend fun getAnyDebt(): DebtEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: DebtEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateDebt(debt: DebtEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteDebt(debt: DebtEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM debts")
    suspend fun deleteAllDebts()

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    // Context methods
    @Query("SELECT * FROM contexts")
    fun getAllContexts(): Flow<List<ContextEntity>>

    @Query("SELECT * FROM contexts WHERE isHidden = 0")
    fun getVisibleContexts(): Flow<List<ContextEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContext(context: ContextEntity)

    @Update
    suspend fun updateContext(context: ContextEntity)

    @Delete
    suspend fun deleteContext(context: ContextEntity)

    @Query("UPDATE debts SET context = 'General' WHERE context = :oldContext")
    suspend fun reassignDebtsToGeneral(oldContext: String)

    @Query("DELETE FROM contexts")
    suspend fun deleteAllContexts()

    @Query("SELECT COUNT(*) FROM contexts")
    suspend fun getContextCount(): Int

    @Query("SELECT COUNT(*) FROM debts WHERE context = :contextName")
    suspend fun getDebtCountByContext(contextName: String): Int
}

@Database(entities = [DebtEntity::class, TransactionEntity::class, ContextEntity::class], version = 13, exportSchema = false)
abstract class DebtDatabase : RoomDatabase() {
    abstract fun debtDao(): DebtDao

    companion object {
        @Volatile
        private var INSTANCE: DebtDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DebtDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DebtDatabase::class.java,
                    "debt_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                
                scope.launch(Dispatchers.IO) {
                    val dao = instance.debtDao()
                    
                    // Always ensure default contexts exist with correct casing
                    listOf("General", "Work", "Family", "Friends").forEach {
                        dao.insertContext(ContextEntity(it))
                    }
                }
                
                instance
            }
        }

        fun resetInstance() {
            INSTANCE = null
        }
    }
}
