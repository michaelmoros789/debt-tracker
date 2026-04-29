package com.michaelmoros.debttracker.ui.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.michaelmoros.debttracker.ui.FrictionConfirmDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    ledgerCount: Int,
    ledgerUris: List<Uri>,
    onNavigateToDisplay: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToManageContext: () -> Unit,
    onNavigateToManageBackups: () -> Unit,
    onNavigateToExportNaming: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onPurgeLedgers: () -> Unit,
    onResetDefaults: () -> Unit,
    onBack: () -> Unit
) {
    var showResetDialog by remember { mutableStateOf(false) }
    var showPurgeSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // Automatically dismiss our sheet if the user leaves the app
                showPurgeSheet = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                SettingsSectionHeader("Appearance")
                SettingsItem(
                    title = "Display Settings",
                    subtitle = "Change item size and font scale",
                    icon = Icons.Default.AspectRatio,
                    onClick = onNavigateToDisplay
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    title = "Theme Settings",
                    subtitle = "Switch between light, dark, or system theme",
                    icon = Icons.Default.Brightness4,
                    onClick = onNavigateToTheme
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    title = "Currency",
                    subtitle = "Choose your preferred currency symbol",
                    icon = Icons.Default.Payments,
                    onClick = onNavigateToCurrency
                )
            }
            item {
                SettingsSectionHeader("Data Management")
                SettingsItem(
                    title = "Manage Contexts",
                    subtitle = "Add, remove or hide categories",
                    icon = Icons.Default.Category,
                    onClick = onNavigateToManageContext
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    title = "Backup & Restore",
                    subtitle = "Export or import your database",
                    icon = Icons.Default.Backup,
                    onClick = onNavigateToManageBackups
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    title = "Export Naming Convention",
                    subtitle = "Choose how backup files are named",
                    icon = Icons.Default.DriveFileRenameOutline,
                    onClick = onNavigateToExportNaming
                )
            }
            item {
                SettingsSectionHeader("Storage & Privacy")
                SettingsItem(
                    title = "Purge Generated Images",
                    subtitle = "Found $ledgerCount images generated by this app",
                    icon = Icons.Default.CleaningServices,
                    onClick = {
                        if (ledgerCount > 0) {
                            showPurgeSheet = true
                        } else {
                            onPurgeLedgers() 
                        }
                    }
                )
            }
            item {
                SettingsSectionHeader("Other")
                SettingsItem(
                    title = "About",
                    subtitle = "App info and developer details",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsItem(
                    title = "Clear All Data",
                    subtitle = "Delete all entries, transactions, and custom categories",
                    icon = Icons.Default.DeleteForever,
                    iconColor = MaterialTheme.colorScheme.error,
                    onClick = { showResetDialog = true }
                )
            }
        }
    }

    if (showPurgeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPurgeSheet = false },
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            PurgeConfirmationSheet(
                uris = ledgerUris,
                onProceed = {
                    showPurgeSheet = false
                    onPurgeLedgers()
                },
                onCancel = { showPurgeSheet = false }
            )
        }
    }

    FrictionConfirmDialog(
        show = showResetDialog,
        title = "Clear All Data?",
        message = "This action is equivalent to resetting the app. All people, transaction histories, and custom categories will be PERMANENTLY DELETED. This cannot be undone.",
        requiredPhrase = "reset-everything",
        confirmButtonText = "CLEAR EVERYTHING",
        isDestructive = true,
        onDismiss = { showResetDialog = false },
        onConfirm = {
            onResetDefaults()
            showResetDialog = false
        }
    )
}

@Composable
fun PurgeConfirmationSheet(
    uris: List<Uri>,
    onProceed: () -> Unit,
    onCancel: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.3f
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Review Purge List",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "The following ${uris.size} images will be permanently removed from your gallery.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))

        Box(modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeight)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uris) { uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("CANCEL", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onProceed,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text("CONTINUE", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 0.dp, bottom = 12.dp)
    )
}

@Composable
private fun SettingsItem(
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
