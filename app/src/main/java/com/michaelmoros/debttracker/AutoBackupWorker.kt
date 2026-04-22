package com.michaelmoros.debttracker

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AutoBackupWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = coroutineScope {
        try {
            val dbName = "debt_database"
            val dbFile = applicationContext.getDatabasePath(dbName)
            if (!dbFile.exists()) return@coroutineScope Result.success()

            // 1. Force Checkpoint to merge WAL into main DB file
            val db = DebtDatabase.getDatabase(applicationContext, this)
            db.query("PRAGMA wal_checkpoint(FULL)", null).close()

            // 2. Setup Backup Directory
            val backupDir = File(applicationContext.filesDir, "auto_backups")
            if (!backupDir.exists()) backupDir.mkdirs()

            // 3. Create New Backup
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_$timestamp.db")
            dbFile.inputStream().use { input ->
                backupFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 4. Rotation Logic: Keep only the latest 5 backups
            val backups = backupDir.listFiles()?.sortedByDescending { it.lastModified() }
            if (backups != null && backups.size > 5) {
                backups.drop(5).forEach { it.delete() }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "auto_backup_work"

        fun getLastBackupTime(context: Context): String {
            val backupDir = File(context.filesDir, "auto_backups")
            val latestFile = backupDir.listFiles()?.maxByOrNull { it.lastModified() }
            return if (latestFile != null) {
                val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(latestFile.lastModified()))
            } else {
                "Never"
            }
        }

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresCharging(true)
                .setRequiresDeviceIdle(true)
                .build()

            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
