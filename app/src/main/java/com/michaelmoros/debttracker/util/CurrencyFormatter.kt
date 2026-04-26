package com.michaelmoros.debttracker.util

import java.util.*
import kotlin.math.abs

object CurrencyFormatter {
    /**
     * Formats an amount in minor units (cents) with abbreviations for large numbers.
     * Example: 100000 (1,000.00) -> $1.0k
     */
    fun formatAmount(amountCents: Long, currencySymbol: String, showSettled: Boolean = true): String {
        if (amountCents == 0L && showSettled) return "Settled"
        
        val absAmount = abs(amountCents / 100.0)
        
        return when {
            absAmount >= 1_000_000_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.1fB", absAmount / 1_000_000_000.0)}"
            absAmount >= 1_000_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.1fM", absAmount / 1_000_000.0)}"
            absAmount >= 100_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.0fk", absAmount / 1_000.0)}"
            else -> "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", absAmount)}"
        }
    }

    /**
     * Formats an amount in minor units (cents) with standard comma separators.
     * Example: 100000 -> $1,000.00
     */
    fun formatStandard(amountCents: Long, currencySymbol: String): String {
        val absAmount = abs(amountCents / 100.0)
        return "$currencySymbol${String.format(Locale.getDefault(), "%,.2f", absAmount)}"
    }
}
