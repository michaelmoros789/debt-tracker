package com.michaelmoros.debttracker.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michaelmoros.debttracker.*
import com.michaelmoros.debttracker.ui.settings.ExportNamingConvention
import com.michaelmoros.debttracker.util.DebtStatementGenerator
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    personId: Long,
    namingConvention: ExportNamingConvention,
    currencySymbol: String,
    onBack: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onShowNotification: (String, NotificationType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { DebtDatabase.getDatabase(context, scope) }
    val dao = db.debtDao()

    val debtWithTransactions by dao.getDebtWithTransactions(personId).collectAsState(initial = null)
    val transactions by dao.getTransactionsForPerson(personId).collectAsState(initial = emptyList())
    val allContexts by dao.getAllContexts().collectAsState(initial = emptyList())

    var showTransactionSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var showChangeCategoryDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // Search State
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    
    val filteredTransactions = remember(transactions, searchQuery) {
        if (searchQuery.isBlank()) transactions
        else {
            // Normalize query: remove commas and currency symbols for better numeric matching
            val normalizedQuery = searchQuery.replace(",", "").replace(currencySymbol, "").trim()
            
            transactions.filter {
                val amountStr = String.format(Locale.getDefault(), "%.2f", abs(it.amount) / 100.0)
                val dateStr = it.date.toFormattedDate() // Use the UTC formatter
                
                it.description.contains(searchQuery, ignoreCase = true) ||
                it.method.contains(searchQuery, ignoreCase = true) ||
                it.referenceNumber?.contains(searchQuery, ignoreCase = true) == true ||
                amountStr.contains(normalizedQuery) ||
                dateStr.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val exportSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val createCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val dwt = debtWithTransactions ?: return@launch
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val csvHeader = "Date,Description,Method,Amount"
                    val csvLines = dwt.transactions.sortedBy { it.date }.map { tx ->
                        val date = sdf.format(Date(tx.date))
                        val escapedDesc = tx.description.replace("\"", "\"\"")
                        val displayAmount = tx.amount / 100.0
                        "\"$date\",\"$escapedDesc\",\"${tx.method}\",$displayAmount"
                    }
                    val csvContent = (listOf(csvHeader) + csvLines).joinToString("\n")

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(csvContent.toByteArray())
                    }
                    onShowNotification("CSV exported successfully", NotificationType.SUCCESS)
                } catch (e: Exception) {
                    onShowNotification("Failed to export CSV: ${e.message}", NotificationType.ERROR)
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
                    val dwt = debtWithTransactions ?: return@launch
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val jsonArray = JSONArray()
                    dwt.transactions.sortedBy { it.date }.forEach { tx ->
                        val obj = JSONObject()
                        obj.put("Date", sdf.format(Date(tx.date)))
                        obj.put("Description", tx.description)
                        obj.put("Method", tx.method)
                        obj.put("Amount", tx.amount / 100.0)
                        jsonArray.put(obj)
                    }
                    val jsonContent = jsonArray.toString(4)

                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(jsonContent.toByteArray())
                    }
                    onShowNotification("JSON exported successfully", NotificationType.SUCCESS)
                } catch (e: Exception) {
                    onShowNotification("Failed to export JSON: ${e.message}", NotificationType.ERROR)
                }
            }
        }
    }

    if (showTransactionSheet || showExportSheet || isSearching) {
        BackHandler(enabled = true) {
            if (isSearching) {
                isSearching = false
                searchQuery = ""
            } else if (isKeyboardVisible) {
                focusManager.clearFocus()
            } else {
                showTransactionSheet = false
                showExportSheet = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search description, date, or amount...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        debtWithTransactions?.debt?.let { debt ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showChangeCategoryDialog = true }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                    .testTag("history_header")
                            ) {
                                Text(
                                    text = debt.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier
                                        .weight(1f, fill = false)
                                        .testTag("history_person_name")
                                )
                                Text(
                                    text = " / ${debt.context}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        } ?: Text("History")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearching) {
                            isSearching = false
                            searchQuery = ""
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isSearching) {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    } else {
                        if (transactions.isNotEmpty()) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        IconButton(
                            onClick = { showTransactionSheet = true },
                            modifier = Modifier.testTag("add_transaction_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                        }
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("more_options_button")
                            ) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More options")
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                if (transactions.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("Export Data") },
                                        onClick = {
                                            showMoreMenu = false
                                            showExportSheet = true
                                        },
                                        leadingIcon = { Icon(Icons.Default.Output, contentDescription = null) }
                                    )
                                }
                                DropdownMenuItem(
                                    text = { Text("Delete Person") },
                                    modifier = Modifier.testTag("delete_person_menu_item"),
                                    onClick = {
                                        showMoreMenu = false
                                        if (transactions.isEmpty()) {
                                            scope.launch {
                                                debtWithTransactions?.debt?.let {
                                                    dao.deleteDebt(it)
                                                    onBack()
                                                    onShowNotification("Entry deleted successfully", NotificationType.SUCCESS)
                                                }
                                            }
                                        } else {
                                            showDeleteDialog = true
                                        }
                                    },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                )
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValue ->
        debtWithTransactions?.let { dwt ->
            val d = dwt.debt
            val balance = transactions.sumOf { it.amount }
            
            Column(modifier = Modifier.padding(paddingValue).fillMaxSize()) {
                if (!isSearching && transactions.isNotEmpty()) {
                    BalanceCard(name = d.name, balance = balance, transactions = transactions, currencySymbol = currencySymbol)
                }

                when {
                    transactions.isEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp).offset(y = (-40).dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    modifier = Modifier.size(80.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(24.dp))
                                Text(
                                    text = "History is empty",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.testTag("empty_history_text")
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Start by adding the first transaction for ${d.name}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(32.dp))
                                Button(
                                    onClick = { showTransactionSheet = true },
                                    shape = RoundedCornerShape(16.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                    modifier = Modifier.testTag("empty_history_add_transaction_button")
                                ) {
                                    Icon(Icons.Default.Add, null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("ADD TRANSACTION")
                                }
                            }
                        }
                    }
                    filteredTransactions.isEmpty() && searchQuery.isNotEmpty() -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.SearchOff, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "No transactions match your search",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f).testTag("transaction_list")) {
                            items(filteredTransactions) { tx ->
                                TransactionItem(
                                    transaction = tx,
                                    currencySymbol = currencySymbol,
                                    onDelete = {
                                        scope.launch { dao.deleteTransaction(tx) }
                                    },
                                    onClick = { onTransactionClick(tx.id) }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showChangeCategoryDialog && debtWithTransactions != null) {
            ChangeCategoryDialog(
                currentCategory = debtWithTransactions!!.debt.context,
                availableCategories = allContexts.filter { !it.isHidden }.map { it.name },
                onDismiss = { showChangeCategoryDialog = false },
                onConfirm = { newCategory ->
                    scope.launch {
                        val updatedDebt = debtWithTransactions!!.debt.copy(context = newCategory)
                        dao.updateDebt(updatedDebt)
                        showChangeCategoryDialog = false
                        onShowNotification("Category updated to $newCategory", NotificationType.SUCCESS)
                    }
                }
            )
        }

        val personName = debtWithTransactions?.debt?.name ?: ""
        val contextName = debtWithTransactions?.debt?.context ?: ""
        FrictionConfirmDialog(
            show = showDeleteDialog,
            title = "Delete Entry",
            message = "Are you sure you want to delete $personName ($contextName)? All transactions will be removed.",
            requiredPhrase = "delete-$personName",
            confirmButtonText = "DELETE",
            isDestructive = true,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    debtWithTransactions?.debt?.let { dao.deleteDebt(it) }
                    showDeleteDialog = false
                    onBack()
                    onShowNotification("Entry deleted successfully", NotificationType.SUCCESS)
                }
            }
        )

        if (showTransactionSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTransactionSheet = false },
                sheetState = sheetState
            ) {
                val balance = transactions.sumOf { it.amount }
                AddTransactionContent(
                    personName = debtWithTransactions?.debt?.name ?: "Person",
                    currentBalance = balance,
                    currencySymbol = currencySymbol,
                    onAdd = { amount: Double, description: String, method: String, reference: String?, isPositive: Boolean, date: Long ->
                        scope.launch {
                            val amountInMinorUnits = (amount * 100).toLong()
                            dao.insertTransaction(TransactionEntity(
                                debtId = personId,
                                amount = if (isPositive) amountInMinorUnits else -amountInMinorUnits,
                                description = description,
                                method = method,
                                referenceNumber = reference,
                                date = date
                            ))
                            sheetState.hide()
                            showTransactionSheet = false
                        }
                    },
                    onCancel = {
                        scope.launch {
                            sheetState.hide()
                            showTransactionSheet = false
                        }
                    }
                )
            }
        }

        if (showExportSheet) {
            ModalBottomSheet(
                onDismissRequest = { showExportSheet = false },
                sheetState = exportSheetState
            ) {
                ExportSheetContent(
                    onDismiss = {
                        scope.launch {
                            exportSheetState.hide()
                            showExportSheet = false
                        }
                    },
                    onExportCsv = {
                        scope.launch {
                            exportSheetState.hide()
                            showExportSheet = false
                            val name = debtWithTransactions?.debt?.name ?: "History"
                            val fileName = namingConvention.formatFileName(name.replace(" ", "_"), "csv", Date())
                            createCsvLauncher.launch(fileName)
                        }
                    },
                    onExportJson = {
                        scope.launch {
                            exportSheetState.hide()
                            showExportSheet = false
                            val name = debtWithTransactions?.debt?.name ?: "History"
                            val fileName = namingConvention.formatFileName(name.replace(" ", "_"), "json", Date())
                            createJsonLauncher.launch(fileName)
                        }
                    },
                    onGenerateFullHistory = {
                        scope.launch {
                            exportSheetState.hide()
                            showExportSheet = false
                            debtWithTransactions?.let { dwt ->
                                DebtStatementGenerator.generateAndSave(
                                    context,
                                    dwt.debt,
                                    dwt.transactions,
                                    namingConvention,
                                    currencySymbol
                                ) { success, message ->
                                    onShowNotification(message, if (success) NotificationType.SUCCESS else NotificationType.ERROR)
                                }
                            }
                        }
                    },
                    onGenerateSinceLastSettled = {
                        scope.launch {
                            exportSheetState.hide()
                            showExportSheet = false
                            debtWithTransactions?.let { dwt ->
                                val filtered = getTransactionsSinceLastSettled(dwt.transactions)
                                DebtStatementGenerator.generateAndSave(
                                    context,
                                    dwt.debt,
                                    filtered,
                                    namingConvention,
                                    currencySymbol
                                ) { success, message ->
                                    onShowNotification(message, if (success) NotificationType.SUCCESS else NotificationType.ERROR)
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChangeCategoryDialog(
    currentCategory: String,
    availableCategories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(currentCategory) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Category") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(
                    "Moving this person to a different context will affect how they are filtered in the main list.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth().testTag("category_selector_card"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(selectedCategory, fontWeight = FontWeight.Bold)
                            Icon(Icons.Default.ArrowDropDown, null)
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f).testTag("change_category_dropdown")
                    ) {
                        availableCategories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category) },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (category == selectedCategory) {
                                        Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                },
                                modifier = Modifier.testTag("category_option_$category")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedCategory) },
                enabled = selectedCategory != currentCategory
            ) {
                Text("UPDATE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL")
            }
        }
    )
}

@Composable
private fun ExportSheetContent(
    onDismiss: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
    onGenerateFullHistory: () -> Unit,
    onGenerateSinceLastSettled: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Export Data",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }

        HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp))

        Text(
            text = "DATA FILES",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 24.dp, bottom = 8.dp)
        )

        ListItem(
            headlineContent = { 
                Text(
                    "Export as CSV", 
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                ) 
            },
            supportingContent = { 
                Text(
                    "Table format for Excel or Google Sheets",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                ) 
            },
            leadingContent = { Icon(Icons.Default.TableChart, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onExportCsv() }
        )

        ListItem(
            headlineContent = { 
                Text(
                    "Export as JSON",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                ) 
            },
            supportingContent = { 
                Text(
                    "Raw data for developers or backup",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                ) 
            },
            leadingContent = { Icon(Icons.Default.Code, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onExportJson() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "VISUAL STATEMENTS",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 8.dp)
        )

        ListItem(
            headlineContent = { 
                Text(
                    "Full History",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                ) 
            },
            supportingContent = { 
                Text(
                    "Export every recorded transaction.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                ) 
            },
            leadingContent = { Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onGenerateFullHistory() }
        )

        ListItem(
            headlineContent = { 
                Text(
                    "Since Last Settled",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                ) 
            },
            supportingContent = { 
                Text(
                    "Focus only on the current active debt cycle.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Normal
                ) 
            },
            leadingContent = { Icon(Icons.Outlined.Update, null, tint = MaterialTheme.colorScheme.primary) },
            modifier = Modifier.clickable { onGenerateSinceLastSettled() }
        )
    }
}

private fun getTransactionsSinceLastSettled(transactions: List<TransactionEntity>): List<TransactionEntity> {
    val sorted = transactions.sortedBy { it.date }
    var lastZeroIndex = -1
    var runningBalance = 0L
    
    for (i in sorted.indices) {
        runningBalance += sorted[i].amount
        if (runningBalance == 0L) {
            lastZeroIndex = i
        }
    }
    
    // Return transactions after the last time balance hit zero
    return if (lastZeroIndex == -1) {
        sorted // Never hit zero
    } else if (lastZeroIndex == sorted.lastIndex) {
        listOf(sorted.last()) // Zero is the latest, just show the last one (settled)
    } else {
        sorted.subList(lastZeroIndex + 1, sorted.size)
    }
}

@Composable
fun BalanceCard(name: String, balance: Long, transactions: List<TransactionEntity>, currencySymbol: String) {
    val themeColor = when {
        balance > 0 -> Color(0xFF2E7D32)
        balance < 0 -> Color(0xFFD32F2F)
        else -> if (transactions.isNotEmpty()) Color(0xFF2E7D32) else Color.Gray
    }
    val lastActivity = transactions.maxByOrNull { it.date }?.date ?: 0L

    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.04f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            val displayBalance = String.format(Locale.getDefault(), "%,.2f", abs(balance) / 100.0)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val statusText = when {
                        balance > 0 -> "$name owes you"
                        balance < 0 -> "You owe $name"
                        transactions.isNotEmpty() -> "All settled"
                        else -> "No transactions yet"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelMedium,
                        color = themeColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (balance == 0L && transactions.isNotEmpty()) "Paid" else "$currencySymbol$displayBalance",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = themeColor
                    )
                }
            }

            if (transactions.isNotEmpty()) {
                val totalLent = transactions.filter { it.amount > 0 }.sumOf { it.amount } / 100.0
                val totalBorrowed = abs(transactions.filter { it.amount < 0 }.sumOf { it.amount }) / 100.0
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.1f))
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Relationship Volume",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "↑$currencySymbol${String.format("%,.0f", totalLent)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "↓$currencySymbol${String.format("%,.0f", totalBorrowed)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color(0xFFD32F2F),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.QueryStats, contentDescription = null, tint = themeColor, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Last activity recorded ", style = MaterialTheme.typography.bodyMedium)
                        Text(lastActivity.toDaysAgo(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = themeColor)
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    transaction: TransactionEntity, 
    currencySymbol: String, 
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { showMenu = true }
                )
            },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Box {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isOwedToMe = transaction.amount > 0
                val icon = if (isOwedToMe) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward
                val iconColor = if (isOwedToMe) Color(0xFF2E7D32) else Color(0xFFD32F2F)

                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = iconColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        transaction.description,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "${transaction.date.toFormattedDate()} • ${transaction.method}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    transaction.referenceNumber?.let {
                        if (it.isNotEmpty()) {
                            Text(
                                "Ref: $it",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                val displayAmount = String.format("%,.2f", abs(transaction.amount) / 100.0)
                Text(
                    "$currencySymbol$displayAmount",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = iconColor
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Delete Transaction") },
                    onClick = {
                        onDelete()
                        showMenu = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionContent(
    personName: String,
    currentBalance: Long,
    currencySymbol: String,
    onAdd: (Double, String, String, String?, Boolean, Long) -> Unit,
    onCancel: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var isPositive by remember { mutableStateOf(true) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Return true only if the date is today or in the past (UTC)
                val todayUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                    set(Calendar.HOUR_OF_DAY, 23)
                    set(Calendar.MINUTE, 59)
                    set(Calendar.SECOND, 59)
                    set(Calendar.MILLISECOND, 999)
                }.timeInMillis
                return utcTimeMillis <= todayUtc
            }

            override fun isSelectableYear(year: Int): Boolean {
                return year <= Calendar.getInstance().get(Calendar.YEAR)
            }
        }
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    val methods = listOf("Cash", "E-wallet", "Bank Transfer", "Debt Offset")

    val lentLabel = when {
        currentBalance < 0 -> "Repay"
        currentBalance > 0 -> "Lend More"
        else -> "Lent"
    }
    val borrowedLabel = when {
        currentBalance < 0 -> "Borrow More"
        currentBalance > 0 -> "Received"
        else -> "Borrowed"
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("CANCEL")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(scrollState) // FIX: Enable scrolling when keyboard squashes content
    ) {
        Text(
            "New Transaction",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Recording for $personName",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = isPositive,
                onClick = { isPositive = true },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                icon = { Icon(Icons.Default.ArrowUpward, null) }
            ) {
                Text(lentLabel)
            }
            SegmentedButton(
                selected = !isPositive,
                onClick = { isPositive = false },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                icon = { Icon(Icons.Default.ArrowDownward, null) }
            ) {
                Text(borrowedLabel)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                // Smart Currency Validation:
                // 1. Prevents leading zeros (unless it's "0.")
                // 2. Limit to 2 decimal places
                // 3. Cap at 999,999,999.99
                val regex = Regex("^\\d*(\\.\\d{0,2})?$")
                
                var processedInput = input
                if (input.length > 1 && input.startsWith("0") && input[1] != '.') {
                    processedInput = input.substring(1)
                }

                if (processedInput.isEmpty() || (processedInput.matches(regex) && (processedInput.toDoubleOrNull() ?: 0.0) <= 999999999.99)) {
                    amount = processedInput
                }
            },
            label = { Text("Amount") },
            prefix = { Text(currencySymbol) },
            visualTransformation = CurrencyVisualTransformation(),
            modifier = Modifier.fillMaxWidth().testTag("transaction_amount_field"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
            singleLine = true,
            supportingText = {
                if (amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0) {
                    Text("Maximum allowed: 999,999,999.99", style = MaterialTheme.typography.labelSmall)
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= 50) description = it },
            label = { Text("Description") },
            placeholder = { Text("e.g. Lunch, Movie, Beet seeds") },
            modifier = Modifier.fillMaxWidth().testTag("transaction_description_field"),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            supportingText = {
                Text(
                    text = "${description.length} / 50",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = datePickerState.selectedDateMillis?.toFormattedDate() ?: "Select Date",
            onValueChange = {},
            readOnly = true,
            label = { Text("Transaction Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Select Date")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )

        Spacer(modifier = Modifier.height(12.dp))

        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = method,
                onValueChange = {},
                readOnly = true,
                label = { Text("Payment Method") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth().testTag("transaction_method_field")
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                methods.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            method = selection
                            expanded = false
                        },
                        modifier = Modifier.testTag("method_option_$selection")
                    )
                }
            }
        }

        if (method == "E-wallet" || method == "Bank Transfer") {
            Spacer(modifier = Modifier.height(12.dp))
            
            val hintText = when (method) {
                "E-wallet" -> "Tip: Prefix with the e-wallet provider (e.g., e-wallet-A-xxx) to make your search more efficient later."
                "Bank Transfer" -> "Tip: Prefix with the bank name (e.g., bank-name-xxx) for cleaner and more organized historical records."
                else -> null
            }

            OutlinedTextField(
                value = reference,
                onValueChange = { reference = it },
                label = { Text("Reference Number (Optional)") },
                modifier = Modifier.fillMaxWidth().testTag("transaction_reference_field"),
                singleLine = true,
                supportingText = hintText?.let { 
                    { 
                        Text(
                            text = it,
                            modifier = Modifier.padding(top = 4.dp)
                        ) 
                    } 
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) {
                Text("CANCEL")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val amt = amount.toDoubleOrNull() ?: 0.0
                    val selectedDate = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    if (amt > 0 && description.isNotEmpty()) {
                        onAdd(amt, description, method, reference.ifEmpty { null }, isPositive, selectedDate)
                    }
                },
                enabled = amount.isNotEmpty() && (amount.toDoubleOrNull() ?: 0.0) > 0 && description.isNotEmpty(),
                modifier = Modifier.testTag("save_transaction_button")
            ) {
                Text("SAVE TRANSACTION")
            }
        }
    }
}

/**
 * Visual Transformation that adds thousand separators (commas) as the user types.
 * It handles decimal points correctly and maps the cursor position accurately.
 */
class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val input = text.text
        if (input.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        // Split by decimal point to handle integer part separately
        val parts = input.split(".")
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) "." + parts[1] else ""

        // Add commas to integer part
        val formattedInteger = if (integerPart.isNotEmpty()) {
            val formatter = DecimalFormat("#,###")
            formatter.format(integerPart.toLong())
        } else {
            ""
        }

        val out = formattedInteger + decimalPart
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                
                // Count how many commas were added before the current cursor position
                var commaCount = 0
                var currentOriginalIndex = 0
                
                // Check each character in the integer part
                for (i in integerPart.indices) {
                    if (currentOriginalIndex >= offset) break
                    
                    // A comma is inserted before this digit if its distance from the END of the integer part is a multiple of 3
                    val distanceToRight = integerPart.length - i
                    if (distanceToRight > 0 && distanceToRight % 3 == 0 && i > 0) {
                        commaCount++
                    }
                    currentOriginalIndex++
                }
                
                return offset + commaCount
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                
                // Count how many commas are actually present before the transformed offset
                var commaCount = 0
                for (i in 0 until offset.coerceAtMost(out.length)) {
                    if (out[i] == ',') commaCount++
                }
                
                return (offset - commaCount).coerceAtLeast(0)
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

fun Long.toFormattedDate(): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC") // FORCE UTC
    return sdf.format(Date(this))
}

fun Long.toDaysAgo(): String {
    val diff = System.currentTimeMillis() - this
    val days = diff / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 30 -> "$days days ago"
        days < 365 -> "${days / 30} months ago"
        else -> "${days / 365} years ago"
    }
}
