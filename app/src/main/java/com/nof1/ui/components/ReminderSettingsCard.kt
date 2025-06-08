package com.nof1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nof1.data.model.ReminderSettings
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.DayOfWeek
import java.time.format.DateTimeFormatter

/**
 * Card component for displaying and managing reminder settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsCard(
    reminders: List<ReminderSettings>,
    onAddReminder: () -> Unit,
    onEditReminder: (ReminderSettings) -> Unit,
    onDeleteReminder: (ReminderSettings) -> Unit,
    onToggleReminder: (ReminderSettings, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onAddReminder) {
                    Icon(Icons.Default.Add, contentDescription = "Add Reminder")
                }
            }
            
            if (reminders.isEmpty()) {
                Text(
                    text = "No reminders set",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                
                reminders.forEach { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        onEdit = { onEditReminder(reminder) },
                        onDelete = { onDeleteReminder(reminder) },
                        onToggle = { enabled -> onToggleReminder(reminder, enabled) },
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderItem(
    reminder: ReminderSettings,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (reminder.isEnabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (reminder.description.isNotBlank()) {
                        Text(
                            text = reminder.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    Text(
                        text = formatReminderSchedule(reminder),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = reminder.isEnabled,
                        onCheckedChange = onToggle,
                        thumbContent = {
                            if (reminder.isEnabled) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            } else {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
                        }
                    )
                    
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun formatReminderSchedule(reminder: ReminderSettings): String {
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    val timeString = reminder.time.format(timeFormatter)
    
    return when (reminder.frequency) {
        ReminderFrequency.ONCE -> "Once at $timeString"
        ReminderFrequency.DAILY -> "Daily at $timeString"
        ReminderFrequency.WEEKLY -> {
            if (reminder.daysOfWeek.isEmpty()) {
                "Weekly at $timeString"
            } else {
                val days = reminder.daysOfWeek.joinToString(", ") { 
                    it.name.lowercase().replaceFirstChar { char -> char.uppercase() }
                }
                "$days at $timeString"
            }
        }
        ReminderFrequency.MONTHLY -> "Monthly at $timeString"
        ReminderFrequency.CUSTOM -> {
            val days = reminder.customFrequencyDays ?: 1
            "Every $days day${if (days != 1) "s" else ""} at $timeString"
        }
    }
}