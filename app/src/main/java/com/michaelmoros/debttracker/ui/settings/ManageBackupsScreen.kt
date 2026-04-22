package com.michaelmoros.debttracker.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.michaelmoros.debttracker.DebtDao
import com.michaelmoros.debttracker.DebtDatabase
import com.michaelmoros.debttracker.ui.FrictionConfirmDialog
import com.michaelmoros.debttracker.ui.NotificationOverlay
import com.michaelmoros.debttracker.ui.NotificationType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageBackupsScreen(
    dao: DebtDao, 
    namingConvention: ExportNamingConvention,
    onRestoreStarted: () -> Unit, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = DebtDatabase.getDatabase(context, scope)
    var showRestoreDialog by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }
    val requiredPhrase = "restore-backup"

    // Custom Notification State
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationType by remember { mutableStateOf(NotificationType.SUCCESS) }
    
    // Internal Loading State
    var isRestoring by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Freeze back button when refreshing
    BackHandler(enabled = isRefreshing) {
        // Do nothing, effectively blocking the back gesture
    }

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val allData = dao.getAllDebtsWithTransactions().first()
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val csvHeader = "Date,Person,Description,Method,Amount,IsOwedToMe"
                    val csvLines = allData.flatMap { debtWithTxs ->
                        val personName = debtWithTxs.debt.name
                        debtWithTxs.transactions.sortedBy { it.date }.map { tx ->
                            val date = sdf.format(Date(tx.date))
                            val escapedDesc = tx.description.replace("\"", "\"\"")
                            "\"$date\",\"$personName\",\"$escapedDesc\",\"${tx.method}\",${tx.amount},${tx.amount > 0}"
                        }
                    }
                    val csvContent = (listOf(csvHeader) + csvLines).joinToString("\n")

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    
                    notificationMessage = "CSV Backup saved successfully"
                    notificationType = NotificationType.SUCCESS
                    showNotification = true
                    delay(2500)
                    showNotification = false
                } catch (e: Exception) {
                    notificationMessage = "Failed to save CSV: ${e.message}"
                    notificationType = NotificationType.ERROR
                    showNotification = true
                    delay(2500)
                    showNotification = false
                }
            }
        }
    }

    val createJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val allData = dao.getAllDebtsWithTransactions().first()
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val jsonArray = JSONArray()
                    allData.forEach { debtWithTxs ->
                        val personName = debtWithTxs.debt.name
                        debtWithTxs.transactions.sortedBy { it.date }.forEach { tx ->
                            val obj = JSONObject()
                            obj.put("Date", sdf.format(Date(tx.date)))
                            obj.put("Person", personName)
                            obj.put("Description", tx.description)
                            obj.put("Method", tx.method)
                            obj.put("Amount", tx.amount)
                            obj.put("IsOwedToMe", tx.amount > 0)
                            jsonArray.put(obj)
                        }
                    }
                    val jsonContent = jsonArray.toString(4)

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                    
                    notificationMessage = "JSON Backup saved successfully"
                    notificationType = NotificationType.SUCCESS
                    showNotification = true
                    delay(2500)
                    showNotification = false
                } catch (e: Exception) {
                    notificationMessage = "Failed to save JSON: ${e.message}"
                    notificationType = NotificationType.ERROR
                    showNotification = true
                    delay(2500)
                    showNotification = false
                }
            }
        }
    }

    val createSqlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/x-sqlite3")
    ) { uri ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    db.query("PRAGMA wal_checkpoint(FULL)", null).use { cursor ->
                        cursor.moveToFirst()
                    }
                    val dbFile = context.getDatabasePath("debt_database")
                    if (dbFile.exists()) {
                        context.contentResolver.openOutputStream(it)?.use { output ->
                            dbFile.inputStream().use { input -> input.copyTo(output) }
                        }
                        withContext(Dispatchers.Main) {
                            notificationMessage = "SQL Database exported successfully"
                            notificationType = NotificationType.SUCCESS
                            showNotification = true
                            delay(2500)
                            showNotification = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        notificationMessage = "Export failed: ${e.message}"
                        notificationType = NotificationType.ERROR
                        showNotification = true
                        delay(2500)
                        showNotification = false
                    }
                }
            }
        }
    }

    val pickSqlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            pendingRestoreUri = it
            showRestoreDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Backup & Restore") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValue ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValue)
                    .fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Export",
                        subtitle = "Save your history and data to a secure file"
                    )
                    BackupButton(
                        title = "Export SQL (.db)",
                        subtitle = "Safely back up all your data to restore it later",
                        icon = Icons.Default.Storage,
                        onClick = {
                            val fileName = namingConvention.formatFileName("DebtLedger", "db", Date())
                            createSqlLauncher.launch(fileName)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BackupButton(
                        title = "Export CSV",
                        subtitle = "Save your history to view in Excel or Google Sheets",
                        icon = Icons.Default.Description,
                        onClick = {
                            val fileName = namingConvention.formatFileName("DebtLedger", "csv", Date())
                            createCsvLauncher.launch(fileName)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BackupButton(
                        title = "Export JSON",
                        subtitle = "Technical format for sharing data with other apps",
                        icon = Icons.Default.Code,
                        onClick = {
                            val fileName = namingConvention.formatFileName("DebtLedger", "json", Date())
                            createJsonLauncher.launch(fileName)
                        }
                    )
                }

                item {
                    SectionHeader(
                        title = "Import",
                        subtitle = "Restore your data from a previous backup file"
                    )
                    BackupButton(
                        title = "Restore SQL (.db)",
                        subtitle = "Restore everything from a previously saved backup file",
                        icon = Icons.Default.Restore,
                        iconColor = MaterialTheme.colorScheme.error,
                        onClick = {
                            pickSqlLauncher.launch(arrayOf("application/x-sqlite3", "application/octet-stream"))
                        }
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }

        // --- Confirmation Dialog ---
        FrictionConfirmDialog(
            show = showRestoreDialog,
            title = "Restore Database?",
            message = "This will OVERWRITE all current data with the selected backup. This cannot be undone.",
            requiredPhrase = requiredPhrase,
            confirmButtonText = if (isRestoring) "RESTORING..." else "RESTORE NOW",
            isDestructive = true,
            onDismiss = { if (!isRestoring) showRestoreDialog = false },
            onConfirm = {
                pendingRestoreUri?.let { uri ->
                    isRestoring = true
                    scope.launch(Dispatchers.IO) {
                        try {
                            // 1. Close and Reset Database Instance
                            db.close()
                            DebtDatabase.resetInstance()
                            
                            val dbFile = context.getDatabasePath("debt_database")
                            
                            // 2. Overwrite the main .db file
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                dbFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            
                            // 3. Delete auxiliary SQLite files to ensure fresh load
                            val shmFile = File(dbFile.path + "-shm")
                            val walFile = File(dbFile.path + "-wal")
                            if (shmFile.exists()) shmFile.delete()
                            if (walFile.exists()) walFile.delete()
                            
                            withContext(Dispatchers.Main) {
                                isRestoring = false
                                showRestoreDialog = false
                                isRefreshing = true
                                
                                notificationType = NotificationType.SUCCESS
                                
                                // Countdown loop
                                for (i in 5 downTo 1) {
                                    notificationMessage = "Restore successful! Refreshing the app in $i..."
                                    showNotification = true
                                    delay(1000)
                                }
                                
                                // Trigger navigation to Splash
                                onRestoreStarted()
                                
                                // 4. Force a clean Activity recreate to re-initialize everything
                                context.findActivity()?.recreate()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isRestoring = false
                                notificationMessage = "Restore failed: ${e.message}"
                                notificationType = NotificationType.ERROR
                                showNotification = true
                                delay(3000)
                                showNotification = false
                            }
                        }
                    }
                }
            }
        )

        // --- Screen Freeze Overlay ---
        if (isRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(5f)
                    .clickable(
                        enabled = true,
                        onClick = { /* Consume clicks */ },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    )
            )
        }

        // --- Standardized Notification Overlay (Last in Box) ---
        NotificationOverlay(
            visible = showNotification,
            message = notificationMessage,
            type = notificationType
        )
    }
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun BackupButton(
    title: String, 
    subtitle: String, 
    icon: ImageVector, 
    iconColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
