package com.nof1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nof1.data.model.ReminderSettings
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Dialog for creating/editing reminder settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    isEdit: Boolean = false,
    initialReminder: ReminderSettings? = null,
    entityType: ReminderEntityType,
    entityId: String,
    projectId: String,
    onDismiss: () -> Unit,
    onSave: (ReminderSettings) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var selectedFrequency by remember { mutableStateOf(initialReminder?.frequency ?: "DAILY") }
    var selectedTime by remember { mutableStateOf(initialReminder?.getNotificationTime() ?: LocalTime.of(9, 0)) }
    var customDays by remember { mutableStateOf(initialReminder?.customFrequencyDays?.toString() ?: "1") }
    var selectedDaysOfWeek by remember { mutableStateOf(initialReminder?.getDaysOfWeekSet() ?: emptySet()) }
    var isEnabled by remember { mutableStateOf(initialReminder?.isEnabled ?: true) }
    
    var showTimePicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isEdit) "Edit Reminder" else "Add Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                
                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Frequency selection
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(bottom = 16.dp)
                ) {
                    val frequencies = listOf(
                        "ONCE" to "Once",
                        "DAILY" to "Daily",
                        "WEEKLY" to "Weekly",
                        "MONTHLY" to "Monthly",
                        "CUSTOM" to "Custom"
                    )
                    
                    frequencies.forEach { (value, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedFrequency == value,
                                    onClick = { selectedFrequency = value },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFrequency == value,
                                onClick = null
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // Custom frequency input
                if (selectedFrequency == "CUSTOM") {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { customDays = it },
                        label = { Text("Every X days") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    )
                }
                
                // Days of week selection for weekly frequency
                if (selectedFrequency == "WEEKLY") {
                    Text(
                        text = "Select Days",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DayOfWeek.values().forEach { day ->
                            FilterChip(
                                selected = selectedDaysOfWeek.contains(day),
                                onClick = {
                                    selectedDaysOfWeek = if (selectedDaysOfWeek.contains(day)) {
                                        selectedDaysOfWeek - day
                                    } else {
                                        selectedDaysOfWeek + day
                                    }
                                },
                                label = { Text(day.name.take(3)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                
                // Time selection
                Text(
                    text = "Time",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = selectedTime.format(DateTimeFormatter.ofPattern("HH:mm")),
                    onValueChange = { },
                    label = { Text("Notification Time") },
                    trailingIcon = {
                        IconButton(onClick = { showTimePicker = true }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Select Time")
                        }
                    },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )
                
                // Enabled toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val reminder = ReminderSettings(
                                id = initialReminder?.id ?: "",
                                entityType = entityType.name,
                                entityId = entityId,
                                projectId = projectId,
                                isEnabled = isEnabled,
                                title = title,
                                description = description,
                                frequency = selectedFrequency,
                                timeHour = selectedTime.hour,
                                timeMinute = selectedTime.minute,
                                customFrequencyDays = if (selectedFrequency == "CUSTOM") {
                                    customDays.toIntOrNull()
                                } else null,
                                daysOfWeek = if (selectedFrequency == "WEEKLY") {
                                    selectedDaysOfWeek.map { it.name }
                                } else emptyList()
                            )
                            onSave(reminder)
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (isEdit) "Save" else "Add")
                    }
                }
            }
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialTime = selectedTime,
            onTimeSelected = { time ->
                selectedTime = time
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * Simple time picker dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hourText by remember { mutableStateOf(initialTime.hour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Time",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    // Hour selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = hourText,
                            onValueChange = { value ->
                                // Allow only digits and limit to 2 characters
                                if (value.length <= 2 && value.all { it.isDigit() }) {
                                    hourText = value
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            singleLine = true
                        )
                    }
                    
                    Text(":", style = MaterialTheme.typography.headlineMedium)
                    
                    // Minute selector
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelSmall)
                        OutlinedTextField(
                            value = minuteText,
                            onValueChange = { value ->
                                // Allow only digits and limit to 2 characters
                                if (value.length <= 2 && value.all { it.isDigit() }) {
                                    minuteText = value
                                }
                            },
                            modifier = Modifier.width(80.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                            singleLine = true
                        )
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 0
                            val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                            val selectedTime = LocalTime.of(hour, minute)
                            onTimeSelected(selectedTime)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}