package com.michaelmoros.debttracker.ui

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

enum class NotificationType {
    SUCCESS, ERROR, INFO
}

@Composable
fun NotificationOverlay(
    visible: Boolean,
    message: String,
    type: NotificationType = NotificationType.SUCCESS,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .zIndex(10f)
        ) {
            // Standard colors for Success (Green), Error (Red), Info (Blue)
            val containerColor = when (type) {
                NotificationType.SUCCESS -> Color(0xFFE8F5E9)
                NotificationType.ERROR -> Color(0xFFFFEBEE)
                NotificationType.INFO -> Color(0xFFE3F2FD)
            }
            
            val contentColor = when (type) {
                NotificationType.SUCCESS -> Color(0xFF2E7D32)
                NotificationType.ERROR -> Color(0xFFD32F2F)
                NotificationType.INFO -> Color(0xFF1976D2)
            }
            
            val icon = when (type) {
                NotificationType.SUCCESS -> Icons.Default.CheckCircle
                NotificationType.ERROR -> Icons.Default.Error
                NotificationType.INFO -> Icons.Default.Info
            }

            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(24.dp)),
                color = containerColor,
                tonalElevation = 4.dp,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    if (actionLabel != null && onActionClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onActionClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Text(
                                text = actionLabel.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
