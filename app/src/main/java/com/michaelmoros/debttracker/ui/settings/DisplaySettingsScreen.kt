package com.michaelmoros.debttracker.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ItemSize(val label: String, val icon: ImageVector) {
    SMALL("Small", Icons.Default.TextFields),
    MEDIUM("Medium", Icons.Default.FormatSize),
    LARGE("Large", Icons.Default.TextIncrease);

    fun toItemHeight(): Dp = when (this) {
        SMALL -> 44.dp
        MEDIUM -> 64.dp
        LARGE -> 88.dp
    }

    fun toFontSize(): TextUnit = when (this) {
        SMALL -> 14.sp
        MEDIUM -> 17.sp
        LARGE -> 20.sp
    }

    fun toPadding(): Dp = when (this) {
        SMALL -> 10.dp
        MEDIUM -> 18.dp
        LARGE -> 26.dp
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplaySettingsScreen(
    currentItemSize: ItemSize,
    onSizeChange: (ItemSize) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()
    
    val animatedPadding by animateDpAsState(
        targetValue = currentItemSize.toPadding(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "previewPadding"
    )
    val animatedFontSize by animateFloatAsState(
        targetValue = currentItemSize.toFontSize().value,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "previewFontSize"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Display Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValue ->
        Column(
            modifier = Modifier
                .padding(paddingValue)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Preview Section
            SectionHeader(
                title = "Interface Preview",
                subtitle = "Visualize how items will appear in the main list"
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Preview Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Ledger View",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Preview Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("NAME", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("BALANCE", style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(80.dp), textAlign = TextAlign.End, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Preview Items
                    Column(modifier = Modifier.fillMaxWidth()) {
                        repeat(3) { i ->
                            PreviewListItem(
                                index = i,
                                animatedPadding = animatedPadding,
                                animatedFontSize = animatedFontSize
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Adjustment Section
            SectionHeader(
                title = "Text & Item Density",
                subtitle = "Choose how much information you want to see at once"
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ItemSize.entries.forEachIndexed { index, size ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = ItemSize.entries.size),
                                onClick = { onSizeChange(size) },
                                selected = currentItemSize == size,
                                icon = { SegmentedButtonDefaults.Icon(active = currentItemSize == size) {
                                    Icon(size.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                }}
                            ) {
                                Text(size.label)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            
            // Helpful tip
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Tip: Use 'Large' if you prefer better readability, or 'Small' to view more people without scrolling.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
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
private fun PreviewListItem(index: Int, animatedPadding: Dp, animatedFontSize: Float) {
    val names = listOf("John Doe", "Jane Smith", "Michael Scott")
    val amounts = listOf("₱1,200.00", "₱450.00", "Settled")
    val colors = listOf(Color(0xFFD32F2F), Color(0xFF2E7D32), Color.Gray)
    
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = animatedPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = names[index % names.size],
                    fontWeight = FontWeight.Bold,
                    fontSize = animatedFontSize.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "General",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = (animatedFontSize * 0.8f).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = amounts[index % amounts.size],
                    fontWeight = FontWeight.Bold,
                    fontSize = animatedFontSize.sp,
                    color = colors[index % colors.size],
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size((animatedFontSize * 1.2f).dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
