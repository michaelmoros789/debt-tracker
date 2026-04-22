package com.michaelmoros.debttracker

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.michaelmoros.debttracker.ui.HistoryScreen
import com.michaelmoros.debttracker.ui.NotificationOverlay
import com.michaelmoros.debttracker.ui.NotificationType
import com.michaelmoros.debttracker.ui.TransactionDetailsScreen
import com.michaelmoros.debttracker.ui.settings.*
import com.michaelmoros.debttracker.ui.theme.MyApplicationTheme
import com.michaelmoros.debttracker.ui.toDaysAgo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    private var isReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !isReady }
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val db = DebtDatabase.getDatabase(this, CoroutineScope(Dispatchers.Main))
        CoroutineScope(Dispatchers.Main).launch {
            db.debtDao().getAllDebts().first()
            isReady = true
        }

        setContent {
            val viewModel: DebtViewModel = viewModel()
            val themeMode by viewModel.themeMode.collectAsState()
            
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }
}

sealed class Screen {
    data object Main : Screen()
    data class History(val personId: Long) : Screen()
    data class TransactionDetails(val transactionId: Long, val fromPersonId: Long) : Screen()
    data object Settings : Screen()
    data object DisplaySettings : Screen()
    data object CurrencySettings : Screen()
    data object ThemeSettings : Screen()
    data object ManageContext : Screen()
    data object ManageBackups : Screen()
    data object ExportNaming : Screen()
    data object About : Screen()
}

@Composable
fun AppNavigation(viewModel: DebtViewModel) {
    val navigationStack = remember { mutableStateListOf<Screen>(Screen.Main) }
    val currentScreen = navigationStack.last()
    
    val itemSize by viewModel.itemSize.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val exportNamingConvention by viewModel.exportNamingConvention.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    
    val context = LocalContext.current

    var notificationVisible by remember { mutableStateOf(false) }
    var notificationMessage by remember { mutableStateOf("") }
    var notificationType by remember { mutableStateOf(NotificationType.SUCCESS) }

    fun showNotification(message: String, type: NotificationType = NotificationType.SUCCESS) {
        notificationMessage = message
        notificationType = type
        notificationVisible = true
    }

    LaunchedEffect(notificationVisible) {
        if (notificationVisible) {
            delay(3000)
            notificationVisible = false
        }
    }

    fun navigateTo(screen: Screen) {
        navigationStack.add(screen)
    }

    fun navigateBack() {
        if (navigationStack.size > 1) {
            navigationStack.removeAt(navigationStack.size - 1)
        } else {
            // Modern UX: Let the system handle exit from the main screen
            (context as? Activity)?.onBackPressed()
        }
    }

    BackHandler {
        navigateBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentScreen,
            label = "ScreenTransition",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { screen ->
            when (screen) {
                is Screen.Main -> MainScreen(
                    viewModel = viewModel,
                    onPersonClick = { navigateTo(Screen.History(it)) },
                    onSettingsClick = { navigateTo(Screen.Settings) }
                )
                is Screen.History -> HistoryScreen(
                    personId = screen.personId,
                    namingConvention = exportNamingConvention,
                    currencySymbol = currencySymbol,
                    onBack = { navigateBack() },
                    onTransactionClick = { navigateTo(Screen.TransactionDetails(it, screen.personId)) },
                    onShowNotification = { msg, type -> showNotification(msg, type) }
                )
                is Screen.TransactionDetails -> TransactionDetailsScreen(
                    transactionId = screen.transactionId,
                    currencySymbol = currencySymbol,
                    onBack = { navigateBack() },
                    onDelete = { tx ->
                        viewModel.deleteTransaction(tx) {
                            showNotification("Transaction deleted", NotificationType.SUCCESS)
                            navigateBack()
                        }
                    }
                )
                is Screen.Settings -> SettingsScreen(
                    onNavigateToDisplay = { navigateTo(Screen.DisplaySettings) },
                    onNavigateToCurrency = { navigateTo(Screen.CurrencySettings) },
                    onNavigateToTheme = { navigateTo(Screen.ThemeSettings) },
                    onNavigateToManageContext = { navigateTo(Screen.ManageContext) },
                    onNavigateToManageBackups = { navigateTo(Screen.ManageBackups) },
                    onNavigateToExportNaming = { navigateTo(Screen.ExportNaming) },
                    onNavigateToAbout = { navigateTo(Screen.About) },
                    onResetDefaults = {
                        viewModel.resetDefaults { msg -> showNotification(msg, NotificationType.SUCCESS) }
                    },
                    onBack = { navigateBack() }
                )
                is Screen.DisplaySettings -> DisplaySettingsScreen(
                    currentItemSize = itemSize,
                    onSizeChange = { viewModel.setItemSize(it) },
                    onBack = { navigateBack() }
                )
                is Screen.CurrencySettings -> CurrencySettingsScreen(
                    currentCurrency = currencySymbol,
                    onCurrencySelected = { viewModel.setCurrencySymbol(it) },
                    onBack = { navigateBack() }
                )
                is Screen.ThemeSettings -> ThemeSettingsScreen(
                    currentThemeMode = themeMode,
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onBack = { navigateBack() }
                )
                is Screen.ManageContext -> ManageContextScreen(
                    onBack = { navigateBack() }
                )
                is Screen.ManageBackups -> ManageBackupsScreen(
                    dao = viewModel.getDao(),
                    namingConvention = exportNamingConvention,
                    onRestoreStarted = { /* Handled elsewhere */ },
                    onBack = { navigateBack() }
                )
                is Screen.ExportNaming -> ExportSettingsScreen(
                    currentConvention = exportNamingConvention,
                    onConventionChange = { viewModel.setExportNamingConvention(it) },
                    onBack = { navigateBack() }
                )
                is Screen.About -> AboutScreen(
                    onBack = { navigateBack() }
                )
            }
        }

        NotificationOverlay(
            visible = notificationVisible,
            message = notificationMessage,
            type = notificationType
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: DebtViewModel,
    onPersonClick: (Long) -> Unit, 
    onSettingsClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val peopleWithBalances by viewModel.sortedPeople.collectAsState()
    val contexts by viewModel.contexts.collectAsState()
    val itemSize by viewModel.itemSize.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val currentSortField by viewModel.currentSortField.collectAsState()
    val currentSortOrder by viewModel.currentSortOrder.collectAsState()

    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusManager = LocalFocusManager.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Search State
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    val filteredPeople = remember(peopleWithBalances, searchQuery, currencySymbol) {
        val query = searchQuery.trim().lowercase()
        if (query.isBlank()) peopleWithBalances
        else {
            peopleWithBalances?.filter { person ->
                val nameMatch = person.debt.name.contains(query, ignoreCase = true)
                val contextMatch = person.debt.context.contains(query, ignoreCase = true)
                
                // Smart balance search logic
                val balanceCents = person.balance
                val balanceAbs = abs(balanceCents)
                val balanceFormatted = formatAmount(balanceCents, currencySymbol).lowercase()
                
                val formattedMatch = balanceFormatted.contains(query)
                
                // Handle unit search (k, M, B)
                val unitMatch = if (query.isNotEmpty() && query.last().isLetter()) {
                    val unit = query.last()
                    val valueStr = query.dropLast(1).trim()
                    val value = valueStr.toDoubleOrNull()
                    if (value != null) {
                        val multiplier = when (unit) {
                            'k' -> 1_000.0
                            'm' -> 1_000_000.0
                            'b' -> 1_000_000_000.0
                            else -> 1.0
                        }
                        val targetCents = (value * multiplier * 100).toLong()
                        // Use a small range for floating point matches (within 0.1% or at least 1 cent)
                        val delta = abs(balanceAbs - targetCents)
                        delta <= (targetCents * 0.001).toLong().coerceAtLeast(1L)
                    } else false
                } else false

                val rawValueMatch = (balanceAbs / 100).toString().contains(query) || 
                                   (balanceAbs / 100.0).toString().contains(query)

                nameMatch || contextMatch || formattedMatch || unitMatch || rawValueMatch
            }
        }
    }

    BackHandler(enabled = showBottomSheet || isSearching) {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else if (isKeyboardVisible) {
            focusManager.clearFocus()
        } else {
            showBottomSheet = false
        }
    }

    fun ItemSize.toPadding() = when (this) {
        ItemSize.SMALL -> 8.dp
        ItemSize.MEDIUM -> 12.dp
        ItemSize.LARGE -> 16.dp
    }

    fun ItemSize.toFontSize() = when (this) {
        ItemSize.SMALL -> 14.sp
        ItemSize.MEDIUM -> 16.sp
        ItemSize.LARGE -> 18.sp
    }

    val animatedPadding by animateDpAsState(
        targetValue = itemSize.toPadding(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "padding"
    )
    val animatedFontSize by animateFloatAsState(
        targetValue = itemSize.toFontSize().value,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "fontSize"
    )

    if (peopleWithBalances == null) {
        Box(modifier = Modifier.fillMaxSize())
        return
    }

    Scaffold(
        topBar = {
            if (isSearching) {
                TopAppBar(
                    title = {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search people or contexts...") },
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
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            isSearching = false
                            searchQuery = ""
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    }
                )
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("Debt Tracker") },
                    actions = {
                        if (peopleWithBalances?.isNotEmpty() == true) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        IconButton(onClick = onSettingsClick) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (peopleWithBalances?.isNotEmpty() == true && !isSearching) {
                ExtendedFloatingActionButton(
                    onClick = { showBottomSheet = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("ADD PERSON") },
                    modifier = Modifier.testTag("add_person_fab")
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (peopleWithBalances?.isEmpty() == true) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.PersonAdd, null, Modifier.size(80.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        Spacer(Modifier.height(24.dp))
                        Text("To start using, you need to add a person", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(32.dp))
                        Button(
                            onClick = { showBottomSheet = true }, 
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.testTag("empty_add_person_button")
                        ) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("ADD PERSON")
                        }
                    }
                }
            } else {
                // Table View Header
                Surface(
                    tonalElevation = 2.dp,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "NAME",
                            modifier = Modifier.weight(1f).clickable { viewModel.onHeaderClick(SortField.NAME) },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSortField == SortField.NAME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "CONTEXT",
                            modifier = Modifier.weight(1f).clickable { viewModel.onHeaderClick(SortField.CONTEXT) },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (currentSortField == SortField.CONTEXT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "BALANCE",
                            modifier = Modifier.width(100.dp).clickable { viewModel.onHeaderClick(SortField.BALANCE) },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.End,
                            color = if (currentSortField == SortField.BALANCE) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (filteredPeople == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (filteredPeople.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(if (searchQuery.isBlank()) "No people added yet." else "No matches found.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredPeople, key = { it.debt.id }) { person ->
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                            PersonItem(
                                person = person,
                                currencySymbol = currencySymbol,
                                nameSize = animatedFontSize.sp,
                                verticalPadding = animatedPadding,
                                onClick = { onPersonClick(person.debt.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            AddPersonSheetContent(
                availableContexts = contexts.map { it.name },
                onAddPerson = { name, context ->
                    viewModel.addPerson(name, context)
                    showBottomSheet = false
                },
                onCancel = { showBottomSheet = false },
                existingPeople = peopleWithBalances?.map { it.debt.name } ?: emptyList()
            )
        }
    }
}

@Composable
fun PersonItem(
    person: DebtWithBalance,
    currencySymbol: String,
    nameSize: androidx.compose.ui.unit.TextUnit,
    verticalPadding: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val balance = person.balance
    val color = when {
        balance > 0 -> Color(0xFF4CAF50)
        balance < 0 -> Color(0xFFF44336)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = verticalPadding)
            .testTag("person_item_${person.debt.name}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = person.debt.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = nameSize),
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = person.debt.context,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val lastDate = person.lastTransactionDate
            if (lastDate != null) {
                val diff = System.currentTimeMillis() - lastDate
                val days = diff / (1000 * 60 * 60 * 24)
                val status = if (days > 30) "Stagnant" else "Active"
                val statusColor = if (status == "Active") Color(0xFF4CAF50) else Color(0xFFFF9800)
                
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "$status • ${lastDate.toDaysAgo()}",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = statusColor
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Gray.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Inactive",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold),
                        color = Color.Gray
                    )
                }
            }
        }
        
        Text(
            text = formatAmount(balance, currencySymbol),
            modifier = Modifier.width(100.dp),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = nameSize),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End,
            color = color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheetContent(
    availableContexts: List<String>,
    onAddPerson: (String, String) -> Unit,
    onCancel: () -> Unit,
    existingPeople: List<String>
) {
    var name by remember { mutableStateOf("") }
    var contextStr by remember { mutableStateOf(if (availableContexts.isNotEmpty()) availableContexts[0] else "Personal") }
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    val isDuplicate = remember(name, existingPeople) {
        existingPeople.any { it.equals(name.trim(), ignoreCase = true) }
    }

    val nameValidationResult = remember(name) {
        val trimmed = name.trim()
        when {
            trimmed.length > 20 -> "Name too long (max 20)"
            else -> null
        }
    }

    Column(modifier = Modifier.padding(16.dp).fillMaxWidth().imePadding()) {
        Text("Add New Person", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name, 
            onValueChange = { name = it }, 
            label = { Text("Name") }, 
            modifier = Modifier.fillMaxWidth().testTag("add_person_name_field"), 
            singleLine = true, 
            isError = isDuplicate || nameValidationResult != null, 
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done), 
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }), 
            supportingText = { Column(Modifier.padding(top = 4.dp)) { if (isDuplicate) Text("Duplicate exists") else if (nameValidationResult != null) Text(nameValidationResult) else Text("${name.trim().length}/20 characters") } }
        )
        Spacer(Modifier.height(16.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = contextStr, 
                onValueChange = {}, 
                readOnly = true, 
                label = { Text("Context") }, 
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, 
                modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth().testTag("add_person_context_field")
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) { availableContexts.forEach { contextName -> DropdownMenuItem(text = { Text(contextName) }, onClick = { contextStr = contextName; expanded = false }, modifier = Modifier.testTag("context_option_$contextName")) } }
        }
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("add_person_cancel_button")) { Text("CANCEL") }
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { if (name.isNotBlank() && !isDuplicate && nameValidationResult == null) onAddPerson(name.trim(), contextStr) }, 
                enabled = name.isNotBlank() && !isDuplicate && nameValidationResult == null,
                modifier = Modifier.testTag("add_person_confirm_button")
            ) { Text("ADD") }
        }
        Spacer(Modifier.height(16.dp))
    }
}

/**
 * Professional Amount Formatter for MVP.
 * Prevents multi-line breakage by using compact notation (k, M, B) for large numbers.
 */
private fun formatAmount(balanceCents: Long, currencySymbol: String): String {
    if (balanceCents == 0L) return "Settled"
    
    val absAmount = abs(balanceCents / 100.0)
    
    return when {
        absAmount >= 1_000_000_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.1fB", absAmount / 1_000_000_000.0)}"
        absAmount >= 1_000_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.1fM", absAmount / 1_000_000.0)}"
        absAmount >= 100_000 -> "$currencySymbol${String.format(Locale.getDefault(), "%.0fk", absAmount / 1_000.0)}"
        else -> "$currencySymbol ${String.format(Locale.getDefault(), "%,.2f", absAmount)}"
    }
}
