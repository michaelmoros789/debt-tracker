package com.michaelmoros.debttracker.util

import android.app.PendingIntent
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

object LedgerManager {
    const val SIGNATURE = "com.michaelmoros.debttracker.generated_ledger"
    private const val RELATIVE_PATH = "Pictures/DebtLedger/"

    fun countGeneratedLedgers(context: Context): Int {
        return try {
            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                getSelection(),
                getSelectionArgs(),
                null
            )
            cursor?.use { it.count } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    fun getLedgerUris(context: Context): List<Uri> {
        val uris = mutableListOf<Uri>()
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                getSelection(),
                getSelectionArgs(),
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    uris.add(ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id))
                }
            }
        } catch (e: Exception) { }
        return uris
    }

    private fun getSelection(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Filter by path and description (Description might be null on some devices, so Path is safer)
            "${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?"
        } else {
            "${MediaStore.Images.Media.DATA} LIKE ?"
        }
    }

    private fun getSelectionArgs(): Array<String> {
        return arrayOf("%DebtLedger%")
    }

    fun createPurgeRequest(context: Context, uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        } else null
    }

    fun purgeLegacy(context: Context, uris: List<Uri>): Int {
        var deletedCount = 0
        for (uri in uris) {
            try {
                if (context.contentResolver.delete(uri, null, null) > 0) deletedCount++
            } catch (e: Exception) { }
        }
        return deletedCount
    }
}
