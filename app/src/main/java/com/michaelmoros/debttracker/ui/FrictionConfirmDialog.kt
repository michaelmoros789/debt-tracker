package com.michaelmoros.debttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.*

@Composable
fun FrictionConfirmDialog(
    show: Boolean,
    title: String,
    message: String,
    requiredPhrase: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonText: String = "CONFIRM",
    isDestructive: Boolean = false
) {
    if (show) {
        var input by remember { mutableStateOf("") }
        
        // Normalize the required phrase: lowercase and replace spaces with hyphens
        val normalizedPhrase = requiredPhrase
            .lowercase(Locale.getDefault())
            .replace("\\s+".toRegex(), "-")
            
        val isConfirmed = input.trim() == normalizedPhrase

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = {
                Column {
                    Text(message)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "To confirm, please type the phrase below:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            text = normalizedPhrase,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(normalizedPhrase) },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isConfirmed,
                    onClick = {
                        onConfirm()
                        input = "" // Reset for next time
                    },
                    colors = if (isDestructive) {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            disabledContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDismiss()
                    input = "" // Reset on cancel
                }) {
                    Text("CANCEL")
                }
            }
        )
    }
}
