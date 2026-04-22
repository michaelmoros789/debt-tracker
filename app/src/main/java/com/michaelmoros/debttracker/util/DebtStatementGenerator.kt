package com.michaelmoros.debttracker.util

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.michaelmoros.debttracker.DebtEntity
import com.michaelmoros.debttracker.TransactionEntity
import com.michaelmoros.debttracker.ui.settings.ExportNamingConvention
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object DebtStatementGenerator {

    private const val W = 1100 // Width to accommodate columns
    private const val TOP_PAD = 44f
    private const val SIDE_PAD = 40f
    private const val TABLE_TOP = 170f
    private const val HEADER_H = 28f
    private const val ROW_H = 34f
    private const val FOOTER_PAD_TOP = 22f
    private const val FOOTER_LINE_H = 16f
    private const val FOOTER_BOTTOM_PAD = 22f

    suspend fun generateAndSave(
        context: Context,
        debt: DebtEntity,
        transactions: List<TransactionEntity>,
        namingConvention: ExportNamingConvention,
        currencySymbol: String,
        onResult: (Boolean, String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
            val sortedTxs = transactions.sortedBy { it.date }
            
            // Computed Rows logic: calculate running balance in minor units
            var running: Long = 0L
            val computedRows = sortedTxs.map { t ->
                running += t.amount
                Triple(t, t.amount, running)
            }
            
            val remaining = if (computedRows.isNotEmpty()) computedRows.last().third else 0L
            val generatedAt = Date()
            
            // Use all transactions for dynamic height
            val rowsToDisplay = computedRows
            val rowCount = rowsToDisplay.size.coerceAtLeast(1) // At least 1 for header/empty state
            
            val tableHeight = HEADER_H + rowCount * ROW_H
            val totalH = (TABLE_TOP + tableHeight + FOOTER_PAD_TOP + FOOTER_LINE_H + FOOTER_BOTTOM_PAD).toInt()

            val bitmap = Bitmap.createBitmap(W, totalH, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Background
            canvas.drawColor(Color.WHITE)
            
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            
            // Formatters
            val dtf = SimpleDateFormat("MMMM d, yyyy (EEEE) hh:mm a", Locale.getDefault())
            val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            /* 1. Header */
            paint.color = Color.parseColor("#111111")
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Transaction History", SIDE_PAD, TOP_PAD, paint)

            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#333333")
            canvas.drawText("Person: ${debt.name}", SIDE_PAD, TOP_PAD + 28, paint)
            canvas.drawText("Context: ${debt.context}", SIDE_PAD, TOP_PAD + 48, paint)
            canvas.drawText("As of: ${dtf.format(generatedAt)}", SIDE_PAD, TOP_PAD + 68, paint)

            /* 2. Divider */
            paint.color = Color.parseColor("#e6e6e6")
            paint.strokeWidth = 1f
            canvas.drawLine(SIDE_PAD, TOP_PAD + 92, W - SIDE_PAD, TOP_PAD + 92, paint)

            /* 3. Summary (Right aligned) */
            val currency = currencySymbol
            val absRemaining = abs(remaining / 100.0)
            val summaryText = when {
                remaining > 0 -> "You owe me: $currency ${String.format(Locale.getDefault(), "%.2f", absRemaining)}"
                remaining < 0 -> "I owe you: $currency ${String.format(Locale.getDefault(), "%.2f", absRemaining)}"
                else -> "Settled: $currency 0.00"
            }
            paint.textSize = 16f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#111111")
            val summaryW = paint.measureText(summaryText)
            canvas.drawText(summaryText, W - SIDE_PAD - summaryW, TOP_PAD + 68, paint)

            /* 4. Table Header */
            val colDateW = 110f
            val colMethodW = 130f
            val colRefW = 180f
            val colAmtW = 160f
            val colBalW = 160f
            val colDescW = W - SIDE_PAD * 2 - colDateW - colMethodW - colRefW - colAmtW - colBalW

            paint.typeface = Typeface.MONOSPACE
            paint.textSize = 14f
            paint.color = Color.parseColor("#666666")
            
            var currentX = SIDE_PAD
            canvas.drawText("DATE", currentX, TABLE_TOP, paint)
            currentX += colDateW
            canvas.drawText("DESC", currentX, TABLE_TOP, paint)
            currentX += colDescW
            canvas.drawText("METHOD", currentX, TABLE_TOP, paint)
            currentX += colMethodW
            canvas.drawText("REF", currentX, TABLE_TOP, paint)
            currentX += colRefW
            canvas.drawText("AMOUNT", currentX, TABLE_TOP, paint)
            currentX += colAmtW
            canvas.drawText("BAL", currentX, TABLE_TOP, paint)

            /* Divider */
            paint.color = Color.parseColor("#e6e6e6")
            canvas.drawLine(SIDE_PAD, TABLE_TOP + 12, W - SIDE_PAD, TABLE_TOP + 12, paint)

            /* 5. Rows */
            if (rowsToDisplay.isEmpty()) {
                paint.color = Color.parseColor("#999999")
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("No transactions found.", SIDE_PAD, TABLE_TOP + HEADER_H + 6, paint)
            } else {
                for (i in rowsToDisplay.indices) {
                    val y = TABLE_TOP + HEADER_H + i * ROW_H
                    val textY = y + 6

                    if (i % 2 == 1) {
                        paint.color = Color.parseColor("#fafafa")
                        canvas.drawRect(SIDE_PAD, y - 18, W - SIDE_PAD, y + 16, paint)
                    }

                    val (t, amount, runningBal) = rowsToDisplay[i]
                    paint.color = Color.parseColor("#111111")
                    paint.typeface = Typeface.MONOSPACE
                    paint.textSize = 14f
                    
                    var rowX = SIDE_PAD
                    // Date
                    canvas.drawText(df.format(Date(t.date)), rowX, textY, paint)
                    rowX += colDateW
                    
                    // Desc
                    val descText = truncate(paint, t.description, colDescW - 12)
                    canvas.drawText(descText, rowX, textY, paint)
                    rowX += colDescW
                    
                    // Method
                    val methodText = truncate(paint, t.method, colMethodW - 12)
                    canvas.drawText(methodText, rowX, textY, paint)
                    rowX += colMethodW
                    
                    // Reference
                    val refText = truncate(paint, t.referenceNumber ?: "-", colRefW - 12)
                    canvas.drawText(refText, rowX, textY, paint)
                    rowX += colRefW
                    
                    // Amount
                    val amtSign = if (amount >= 0) "+" else "-"
                    val amtValue = abs(amount / 100.0)
                    val amtText = "$amtSign$currency ${String.format(Locale.getDefault(), "%.2f", amtValue)}"
                    canvas.drawText(amtText, rowX, textY, paint)
                    rowX += colAmtW
                    
                    // Running Balance
                    val balText = "$currency ${String.format(Locale.getDefault(), "%.2f", runningBal / 100.0)}"
                    canvas.drawText(balText, rowX, textY, paint)

                    paint.color = Color.parseColor("#f0f0f0")
                    canvas.drawLine(SIDE_PAD, y + 16, W - SIDE_PAD, y + 16, paint)
                }
            }

            /* 6. Footer */
            val footerY = TABLE_TOP + tableHeight + FOOTER_PAD_TOP + FOOTER_LINE_H
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 12f
            paint.color = Color.parseColor("#777777")
            canvas.drawText("System-generated for personal tracking.", SIDE_PAD, footerY, paint)

            val baseName = "TransactionHistory_${debt.name.replace(" ", "_")}"
            val fileName = namingConvention.formatFileName(baseName, "png", generatedAt)
            saveToGallery(context, bitmap, fileName, onResult)

        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(false, e.message ?: "Unknown error")
            }
        }
    }

    private fun truncate(paint: Paint, text: String, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ell = "…"
        var t = text
        while (t.isNotEmpty() && paint.measureText(t + ell) > maxWidth) {
            t = t.dropLast(1)
        }
        return t + ell
    }

    private fun saveToGallery(context: Context, bitmap: Bitmap, fileName: String, onResult: (Boolean, String) -> Unit) {
        try {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + File.separator + "DebtLedger")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                } else {
                    val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "DebtLedger")
                    if (!directory.exists()) directory.mkdirs()
                    val file = File(directory, fileName)
                    android.media.MediaScannerConnection.scanFile(context, arrayOf(file.toString()), null, null)
                }
                
                onResult(true, "Statement saved to Gallery")
            } ?: throw Exception("Failed to create MediaStore entry")
        } catch (e: Exception) {
            onResult(false, e.message ?: "Failed to save")
        }
    }
}
