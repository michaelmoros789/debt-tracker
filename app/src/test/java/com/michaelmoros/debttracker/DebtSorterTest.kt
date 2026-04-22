package com.michaelmoros.debttracker

import com.michaelmoros.debttracker.util.DebtSorter
import org.junit.Assert.assertEquals
import org.junit.Test

class DebtSorterTest {

    private val person1 = DebtWithBalance(DebtEntity(id = 1, name = "Zebra", context = "Work"), balance = -1000, transactionCount = 1, lastTransactionDate = null)
    private val person2 = DebtWithBalance(DebtEntity(id = 2, name = "Apple", context = "Home"), balance = 5000, transactionCount = 1, lastTransactionDate = null)
    private val person3 = DebtWithBalance(DebtEntity(id = 3, name = "Mango", context = "Work"), balance = 0, transactionCount = 1, lastTransactionDate = null)
    private val person4 = DebtWithBalance(DebtEntity(id = 4, name = "Banana", context = "Friends"), balance = -5000, transactionCount = 1, lastTransactionDate = null)

    private val people = listOf(person1, person2, person3, person4)

    @Test
    fun `default sort puts debts first, then lent, then settled`() {
        val sorted = DebtSorter.sort(people, null, SortOrder.NONE)
        
        // Expected order: 
        // 1. Banana (-5000) - Debt, largest absolute
        // 2. Zebra (-1000)  - Debt
        // 3. Apple (5000)   - Lent
        // 4. Mango (0)      - Settled
        
        assertEquals(4L, sorted[0].debt.id)
        assertEquals(1L, sorted[1].debt.id)
        assertEquals(2L, sorted[2].debt.id)
        assertEquals(3L, sorted[3].debt.id)
    }

    @Test
    fun `sort by name ascending`() {
        val sorted = DebtSorter.sort(people, SortField.NAME, SortOrder.ASCENDING)
        
        assertEquals("Apple", sorted[0].debt.name)
        assertEquals("Banana", sorted[1].debt.name)
        assertEquals("Mango", sorted[2].debt.name)
        assertEquals("Zebra", sorted[3].debt.name)
    }

    @Test
    fun `sort by balance descending`() {
        val sorted = DebtSorter.sort(people, SortField.BALANCE, SortOrder.DESCENDING)
        
        assertEquals(5000L, sorted[0].balance)
        assertEquals(0L, sorted[1].balance)
        assertEquals(-1000L, sorted[2].balance)
        assertEquals(-5000L, sorted[3].balance)
    }
}
