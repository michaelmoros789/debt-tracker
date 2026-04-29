@file:OptIn(ExperimentalMaterial3Api::class)
package com.michaelmoros.debttracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.michaelmoros.debttracker.DebtViewModel
import com.michaelmoros.debttracker.SortField
import com.michaelmoros.debttracker.SortOrder
import com.michaelmoros.debttracker.util.CurrencyFormatter

@Composable
fun MainScreen(
    viewModel: DebtViewModel,
    onPersonClick: (Long) -> Unit,
    onSettingsClick: () -> Unit
) {
    val sortedPeople by viewModel.sortedPeople.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currentSortField by viewModel.currentSortField.collectAsState()
    val currentSortOrder by viewModel.currentSortOrder.collectAsState()
    val contexts by viewModel.contexts.collectAsState()

    var showAddPersonDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Debt Tracker", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.testTag("Settings")) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            if (sortedPeople.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { showAddPersonDialog = true },
                    modifier = Modifier.testTag("add_person_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Person")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag("search_field"),
                placeholder = { Text("Search by name or category...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            // Header for Sorting
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SortHeaderItem("NAME", SortField.NAME, currentSortField, currentSortOrder, Modifier.weight(1f)) { viewModel.onHeaderClick(it) }
                SortHeaderItem("BALANCE", SortField.BALANCE, currentSortField, currentSortOrder, Modifier.width(100.dp)) { viewModel.onHeaderClick(it) }
            }

            if (sortedPeople.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No entries found",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { showAddPersonDialog = true },
                            modifier = Modifier.testTag("empty_add_person_button")
                        ) {
                            Text("ADD YOUR FIRST PERSON")
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().testTag("person_list")) {
                    items(sortedPeople, key = { it.debt.id }) { debtWithBalance ->
                        PersonListItem(
                            name = debtWithBalance.debt.name,
                            context = debtWithBalance.debt.context,
                            balance = debtWithBalance.balance,
                            currencySymbol = currencySymbol,
                            onClick = { onPersonClick(debtWithBalance.debt.id) }
                        )
                    }
                }
            }
        }

        if (showAddPersonDialog) {
            AddPersonDialog(
                availableContexts = contexts.filter { !it.isHidden }.map { it.name },
                onDismiss = { showAddPersonDialog = false },
                onConfirm = { name, context ->
                    viewModel.addPerson(name, context)
                    showAddPersonDialog = false
                }
            )
        }
    }
}

@Composable
private fun SortHeaderItem(
    label: String,
    field: SortField,
    currentField: SortField?,
    currentOrder: SortOrder,
    modifier: Modifier,
    onClick: (SortField) -> Unit
) {
    Row(
        modifier = modifier.clickable { onClick(field) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (currentField == field) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (currentField == field && currentOrder != SortOrder.NONE) {
            Icon(
                imageVector = if (currentOrder == SortOrder.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun PersonListItem(
    name: String,
    context: String,
    balance: Long,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val color = when {
        balance > 0 -> Color(0xFF2E7D32)
        balance < 0 -> Color(0xFFD32F2F)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("person_item_$name"),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                CurrencyFormatter.formatStandard(balance, currencySymbol),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.End
            )
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

@Composable
private fun AddPersonDialog(
    availableContexts: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedContext by remember { mutableStateOf(availableContexts.firstOrNull() ?: "General") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Person") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth().testTag("add_person_name_field"),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedContext,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().clickable { expanded = true }
                    )
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        availableContexts.forEach { context ->
                            DropdownMenuItem(
                                text = { Text(context) },
                                onClick = {
                                    selectedContext = context
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, selectedContext) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("add_person_confirm_button")
            ) { Text("ADD") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}
