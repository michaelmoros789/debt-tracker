package com.michaelmoros.debttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaelmoros.debttracker.DebtDatabase
import com.michaelmoros.debttracker.TransactionEntity
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    currencySymbol: String,
    onBack: () -> Unit,
    onDelete: (TransactionEntity) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { DebtDatabase.getDatabase(context, scope) }
    val dao = db.debtDao()

    var transaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var personName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        dao.getAllDebtsWithTransactions().collect { allDebts ->
            for (debtWithTx in allDebts) {
                val found = debtWithTx.transactions.find { it.id == transactionId }
                if (found != null) {
                    transaction = found
                    personName = debtWithTx.debt.name
                    break
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transaction Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.testTag("delete_transaction_details_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValue ->
        transaction?.let { tx ->
            val isPositive = tx.amount > 0
            val themeColor = if (isPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
            val displayAmount = String.format(Locale.getDefault(), "%.2f", abs(tx.amount) / 100.0)

            Column(
                modifier = Modifier
                    .padding(paddingValue)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = themeColor.copy(alpha = 0.1f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPositive) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (isPositive) "Lent to $personName" else "Borrowed from $personName",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "$currencySymbol$displayAmount",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = themeColor,
                    modifier = Modifier.testTag("details_amount_value")
                )

                Spacer(modifier = Modifier.height(32.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        DetailItem(
                            label = "Description",
                            value = tx.description,
                            icon = Icons.Default.Description,
                            valueModifier = Modifier.testTag("details_description_value")
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        DetailItem(
                            label = "Transaction Date",
                            value = tx.date.toFullDate(),
                            icon = Icons.Default.Event,
                            valueModifier = Modifier.testTag("details_date_value")
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        DetailItem(
                            label = "Logged At",
                            value = tx.createdAt.toFullDateTime(),
                            icon = Icons.Default.History,
                            isAudit = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                        DetailItem(
                            label = "Payment Method",
                            value = tx.method,
                            icon = Icons.Default.Payments,
                            valueModifier = Modifier.testTag("details_method_value")
                        )
                        
                        tx.referenceNumber?.let {
                            if (it.isNotEmpty()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp)
                                DetailItem(
                                    label = "Reference Number",
                                    value = it,
                                    icon = Icons.Default.ConfirmationNumber,
                                    valueModifier = Modifier.testTag("details_reference_value")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Surface(
                    color = themeColor.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (isPositive) 
                                "This transaction increases the balance $personName owes you." 
                            else 
                                "This transaction increases the amount you owe to $personName.",
                            style = MaterialTheme.typography.bodySmall,
                            color = themeColor
                        )
                    }
                }
            }
        } ?: Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this record? This will adjust the total balance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        transaction?.let { onDelete(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("confirm_delete_transaction_button")
                ) {
                    Text("DELETE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun DetailItem(
    label: String, 
    value: String, 
    icon: ImageVector, 
    modifier: Modifier = Modifier, 
    valueModifier: Modifier = Modifier,
    isAudit: Boolean = false
) {
    Row(verticalAlignment = Alignment.Top, modifier = modifier) {
        Icon(
            icon, 
            contentDescription = null, 
            tint = if (isAudit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary, 
            modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = if (isAudit) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                fontWeight = if (isAudit) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                modifier = valueModifier
            )
        }
    }
}

fun Long.toFullDate(): String {
    val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    sdf.timeZone = TimeZone.getTimeZone("UTC") // FORCE UTC to match DatePicker output
    return sdf.format(Date(this))
}

fun Long.toFullDateTime(): String {
    val sdf = java.text.SimpleDateFormat("MMMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(Date(this))
}
