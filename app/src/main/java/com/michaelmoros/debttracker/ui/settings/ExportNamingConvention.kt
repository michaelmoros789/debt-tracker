package com.michaelmoros.debttracker.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.ui.graphics.vector.ImageVector

enum class ExportNamingConvention(val label: String, val description: String, val icon: ImageVector) {
    DEFAULT(
        "Name + Timestamp",
        "DebtLedger_20260411_1430",
        Icons.Default.Description
    ),
    TIME_FIRST(
        "Timestamp + Name",
        "20260412_0915_DebtLedger",
        Icons.Default.History
    );

    fun formatFileName(baseName: String, extension: String, date: java.util.Date): String {
        val sdf = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
        val time = sdf.format(date)
        return when (this) {
            DEFAULT -> "${baseName}_$time.$extension"
            TIME_FIRST -> "${time}_$baseName.$extension"
        }
    }
}
