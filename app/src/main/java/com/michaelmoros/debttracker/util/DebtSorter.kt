package com.michaelmoros.debttracker.util

import com.michaelmoros.debttracker.DebtWithBalance
import com.michaelmoros.debttracker.SortField
import com.michaelmoros.debttracker.SortOrder
import kotlin.math.abs

object DebtSorter {
    fun sort(
        people: List<DebtWithBalance>,
        field: SortField?,
        order: SortOrder
    ): List<DebtWithBalance> {
        if (order == SortOrder.NONE || field == null) {
            return people.sortedWith(compareBy<DebtWithBalance> {
                val balance = it.balance
                when {
                    balance < 0 -> 0 // Debts (negative) first
                    balance > 0 -> 1 // Lent (positive) second
                    else -> 2        // Settled last
                }
            }.thenByDescending { abs(it.balance) })
        }

        return when (field) {
            SortField.NAME -> if (order == SortOrder.ASCENDING) {
                people.sortedBy { it.debt.name }
            } else {
                people.sortedByDescending { it.debt.name }
            }
            SortField.CONTEXT -> if (order == SortOrder.ASCENDING) {
                people.sortedBy { it.debt.context }
            } else {
                people.sortedByDescending { it.debt.context }
            }
            SortField.BALANCE -> if (order == SortOrder.ASCENDING) {
                people.sortedBy { it.balance }
            } else {
                people.sortedByDescending { it.balance }
            }
            SortField.LAST_TRANSACTION -> if (order == SortOrder.ASCENDING) {
                people.sortedBy { it.lastTransactionDate ?: 0L }
            } else {
                people.sortedByDescending { it.lastTransactionDate ?: 0L }
            }
        }
    }
}
