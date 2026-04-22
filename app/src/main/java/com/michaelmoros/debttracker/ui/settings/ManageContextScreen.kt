package com.michaelmoros.debttracker.ui.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.michaelmoros.debttracker.ContextEntity
import com.michaelmoros.debttracker.DebtDatabase
import com.michaelmoros.debttracker.ui.NotificationOverlay
import com.michaelmoros.debttracker.ui.NotificationType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ManageContextScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { DebtDatabase.getDatabase(context, scope) }
    val dao = db.debtDao()
    val contexts by dao.getAllContexts().collectAsState(initial = emptyList())
    val keyboardController = LocalSoftwareKeyboardController.current
    
    var newContextName by remember { mutableStateOf("") }
    var contextToDelete by remember { mutableStateOf<ContextEntity?>(null) }
    var peopleCountInDeletingContext by remember { mutableIntStateOf(0) }
    
    // Custom Notification State
    var showNotification by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationType by remember { mutableStateOf(NotificationType.SUCCESS) }
    
    val totalCount = contexts.size
    val maxItems = 10
    
    val isLimitReached = totalCount >= maxItems
    val isDuplicate = remember(newContextName, contexts) {
        val formatted = newContextName.trim().lowercase()
        contexts.any { it.name.lowercase() == formatted }
    }
    val isNameTooShort = newContextName.isNotBlank() && newContextName.trim().length < 2
    val canAdd = newContextName.trim().length >= 2 && !isLimitReached && !isDuplicate

    val defaultContexts = listOf("General", "Work", "Family", "Friends")

    LaunchedEffect(contextToDelete) {
        contextToDelete?.let {
            peopleCountInDeletingContext = dao.getDebtCountByContext(it.name)
        }
    }

    suspend fun ensureAtLeastOneVisible() {
        val visibleContexts = dao.getVisibleContexts().first()
        if (visibleContexts.isEmpty()) {
            // Force "General" to be visible if it exists
            val general = dao.getAllContexts().first().find { it.name == "General" }
            general?.let {
                dao.updateContext(it.copy(isHidden = false))
            }
        }
    }

    fun onAddCategory() {
        if (canAdd) {
            val formattedName = newContextName.trim().lowercase()
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            
            scope.launch {
                dao.insertContext(ContextEntity(name = formattedName))
                newContextName = ""
                keyboardController?.hide()
                
                notificationMessage = "Added '$formattedName'"
                notificationType = NotificationType.SUCCESS
                showNotification = true
                delay(2000)
                showNotification = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Manage Contexts") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { paddingValue ->
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .padding(paddingValue)
                    .fillMaxSize()
                    .testTag("context_grid"),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add Category Section (Spans 2 columns)
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Column(modifier = Modifier.padding(top = 0.dp)) {
                        SectionHeader(
                            title = "New Category",
                            subtitle = "Create a new context to organize your entries"
                        )
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Category Name",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$totalCount/$maxItems",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                LinearProgressIndicator(
                                    progress = { totalCount / maxItems.toFloat() },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = if (isLimitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                )

                                Spacer(modifier = Modifier.height(20.dp))
                                
                                Row(verticalAlignment = Alignment.Top) {
                                    OutlinedTextField(
                                        value = newContextName,
                                        onValueChange = { if (it.length <= 20) newContextName = it },
                                        placeholder = { Text("Enemies") },
                                        modifier = Modifier.weight(1f).testTag("new_context_field"),
                                        shape = RoundedCornerShape(16.dp),
                                        singleLine = true,
                                        isError = isNameTooShort || isDuplicate,
                                        supportingText = {
                                            if (isLimitReached) Text("Limit reached")
                                            else if (isDuplicate) Text("Category already exists")
                                            else if (isNameTooShort) Text("Too short")
                                        },
                                        enabled = !isLimitReached,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                        keyboardActions = KeyboardActions(onDone = { onAddCategory() })
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = { onAddCategory() },
                                        enabled = canAdd,
                                        modifier = Modifier.height(56.dp).testTag("add_context_button"),
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Add")
                                    }
                                }
                            }
                        }
                    }
                }

                // Your Categories Section (Spans 2 columns)
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    SectionHeader(
                        title = "Your Categories",
                        subtitle = "Manage visibility or remove your existing contexts"
                    )
                }

                items(contexts, key = { it.name }) { contextEntity ->
                    val isDefault = contextEntity.name in defaultContexts
                    ContextListItemGridCard(
                        contextEntity = contextEntity,
                        isDefault = isDefault,
                        onToggleVisibility = {
                            val isClosing = !contextEntity.isHidden
                            val visibleCount = contexts.count { !it.isHidden }
                            
                            if (isClosing && visibleCount <= 1) {
                                scope.launch {
                                    notificationMessage = "At least one category must be visible"
                                    notificationType = NotificationType.INFO
                                    showNotification = true
                                    delay(2000)
                                    showNotification = false
                                }
                            } else {
                                scope.launch { dao.updateContext(contextEntity.copy(isHidden = !contextEntity.isHidden)) }
                            }
                        },
                        onDelete = {
                            scope.launch {
                                val count = dao.getDebtCountByContext(contextEntity.name)
                                if (count == 0) {
                                    dao.deleteContext(contextEntity)
                                    ensureAtLeastOneVisible()
                                    notificationMessage = "'${contextEntity.name}' deleted"
                                    notificationType = NotificationType.SUCCESS
                                    showNotification = true
                                    delay(2000)
                                    showNotification = false
                                } else {
                                    contextToDelete = contextEntity
                                }
                            }
                        }
                    )
                }

                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }

            if (contextToDelete != null) {
                AlertDialog(
                    onDismissRequest = { contextToDelete = null },
                    title = { Text("Delete Category") },
                    text = {
                        val peopleText = if (peopleCountInDeletingContext == 1) "1 person" else "$peopleCountInDeletingContext people"
                        Text("Are you sure you want to delete '${contextToDelete?.name}'? $peopleText in this category will be moved to 'General'.")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                contextToDelete?.let { entity ->
                                    scope.launch {
                                        // Reassign people to General first
                                        dao.reassignDebtsToGeneral(entity.name)
                                        // Then delete the category
                                        dao.deleteContext(entity)
                                        // Ensure visibility rules
                                        ensureAtLeastOneVisible()
                                        
                                        contextToDelete = null
                                        notificationMessage = "Category deleted & people moved"
                                        notificationType = NotificationType.SUCCESS
                                        showNotification = true
                                        delay(2000)
                                        showNotification = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text("DELETE") }
                    },
                    dismissButton = {
                        TextButton(onClick = { contextToDelete = null }) { Text("CANCEL") }
                    }
                )
            }
        }

        NotificationOverlay(
            visible = showNotification,
            message = notificationMessage,
            type = notificationType
        )
    }
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
private fun ContextListItemGridCard(
    contextEntity: ContextEntity,
    isDefault: Boolean,
    onToggleVisibility: () -> Unit,
    onDelete: () -> Unit
) {
    val contentAlpha by animateFloatAsState(
        targetValue = if (contextEntity.isHidden) 0.5f else 1f,
        label = "contentAlpha"
    )

    Surface(
        onClick = onToggleVisibility,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (contextEntity.isHidden) 0.2f else 0.5f),
        modifier = Modifier.testTag("context_card_${contextEntity.name}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (contextEntity.isHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (contextEntity.isHidden) "VisibilityOff" else "Visibility",
                    tint = if (contextEntity.isHidden) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp).testTag("visibility_icon_${contextEntity.name}")
                )
                
                if (!isDefault) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp).testTag("delete_context_${contextEntity.name}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = contextEntity.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = if (isDefault) "System" else "Custom",
                style = MaterialTheme.typography.labelSmall,
                color = (if (isDefault) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline).copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}
