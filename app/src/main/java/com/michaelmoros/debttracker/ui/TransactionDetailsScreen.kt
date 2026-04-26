package com.michaelmoros.debttracker.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaelmoros.debttracker.DebtDatabase
import com.michaelmoros.debttracker.TransactionEntity
import com.michaelmoros.debttracker.util.CurrencyFormatter
import java.util.*
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailsScreen(
    transactionId: Long,
    currencySymbol: String,
    onBack: () -> Unit,
    onDelete: (TransactionEntity) -> Unit,
    onUpdate: (TransactionEntity) -> Unit
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
            val displayAmount = CurrencyFormatter.formatStandard(tx.amount, currencySymbol)

            // Animation for the arrow rotation
            val rotation by animateFloatAsState(
                targetValue = if (isPositive) 0f else 180f,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "arrow_rotation"
            )

            // Dynamic text size to ensure the amount always fits in one line without ellipsis
            val amountLength = displayAmount.length
            val responsiveFontSize = when {
                amountLength > 20 -> 24.sp
                amountLength > 16 -> 28.sp
                amountLength > 12 -> 36.sp
                else -> 45.sp
            }

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
                            imageVector = Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier
                                .size(40.dp)
                                .rotate(rotation)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = isPositive,
                    label = "text_animation"
                ) { targetPositive ->
                    Text(
                        text = if (targetPositive) "Lent to $personName" else "Borrowed from $personName",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Clip // Ensure full text is seen if possible
                    )
                }

                Text(
                    text = displayAmount,
                    style = MaterialTheme.typography.displayMedium.copy(fontSize = responsiveFontSize),
                    fontWeight = FontWeight.Black,
                    color = themeColor,
                    modifier = Modifier.testTag("details_amount_value"),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Switch Button directly below amount
                OutlinedButton(
                    onClick = { onUpdate(tx.copy(amount = -tx.amount)) },
                    modifier = Modifier.testTag("switch_balance_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(themeColor.copy(alpha = 0.5f))),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.SwapVert, 
                        contentDescription = null, 
                        tint = themeColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SWITCH TRANSACTION DIRECTION",
                        style = MaterialTheme.typography.labelLarge,
                        color = themeColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = "Did you record the wrong transaction type?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    AnimatedContent(
                        targetState = isPositive,
                        transitionSpec = {
                            // Professional fade up animation
                            (slideInVertically { it / 2 } + fadeIn(animationSpec = tween(400))).togetherWith(
                                slideOutVertically { -it / 2 } + fadeOut(animationSpec = tween(400))
                            )
                        },
                        label = "info_card_animation"
                    ) { targetPositive ->
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = if (targetPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (targetPositive) 
                                    "This transaction increases the balance $personName owes you." 
                                else 
                                    "This transaction increases the amount you owe to $personName.",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (targetPositive) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                            )
                        }
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
