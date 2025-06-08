package com.nof1.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.nof1.data.model.ReminderSettings
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.DayOfWeek
import java.time.LocalTime

/**
 * Dialog for creating or editing reminder settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    isEdit: Boolean = false,
    initialReminder: ReminderSettings? = null,
    entityType: ReminderEntityType,
    entityId: Long,
    onDismiss: () -> Unit,
    onSave: (ReminderSettings) -> Unit
) {
    var title by remember { mutableStateOf(initialReminder?.title ?: "") }
    var description by remember { mutableStateOf(initialReminder?.description ?: "") }
    var selectedFrequency by remember { mutableStateOf(initialReminder?.frequency ?: ReminderFrequency.DAILY) }
    var selectedTime by remember { mutableStateOf(initialReminder?.time ?: LocalTime.of(9, 0)) }
    var customDays by remember { mutableStateOf(initialReminder?.customFrequencyDays?.toString() ?: "1") }
    var selectedDaysOfWeek by remember { mutableStateOf(initialReminder?.daysOfWeek ?: emptySet()) }
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
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (isEdit) "Edit Reminder" else "Add Reminder",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Title field
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )
                
                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    maxLines = 3
                )
                
                // Frequency selection
                Text(
                    text = "Frequency",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .padding(bottom = 16.dp)
                ) {
                    ReminderFrequency.values().forEach { frequency ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selectedFrequency == frequency,
                                    onClick = { selectedFrequency = frequency },
                                    role = Role.RadioButton
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedFrequency == frequency,
                                onClick = null
                            )
                            Text(
                                text = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                // Custom frequency days input
                if (selectedFrequency == ReminderFrequency.CUSTOM) {
                    OutlinedTextField(
                        value = customDays,
                        onValueChange = { customDays = it.filter { char -> char.isDigit() } },
                        label = { Text("Days") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        singleLine = true
                    )
                }
                
                // Days of week selection for weekly frequency
                if (selectedFrequency == ReminderFrequency.WEEKLY) {
                    Text(
                        text = "Days of Week",
                        style = MaterialTheme.typography.labelLarge,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time: ${selectedTime.hour.toString().padStart(2, '0')}:${selectedTime.minute.toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("Change")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enable/disable toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Enabled",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
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
                                id = initialReminder?.id ?: 0,
                                entityType = entityType,
                                entityId = entityId,
                                isEnabled = isEnabled,
                                title = title,
                                description = description,
                                frequency = selectedFrequency,
                                time = selectedTime,
                                customFrequencyDays = if (selectedFrequency == ReminderFrequency.CUSTOM) {
                                    customDays.toIntOrNull()
                                } else null,
                                daysOfWeek = if (selectedFrequency == ReminderFrequency.WEEKLY) {
                                    selectedDaysOfWeek
                                } else emptySet()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialTime: LocalTime,
    onTimeSelected: (LocalTime) -> Unit,
    onDismiss: () -> Unit
) {
    var hourText by remember { mutableStateOf(initialTime.hour.toString().padStart(2, '0')) }
    var minuteText by remember { mutableStateOf(initialTime.minute.toString().padStart(2, '0')) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            )
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
                            textStyle = androidx.compose.ui.text.TextStyle(textAlign = TextAlign.Center),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: 0
                    val minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: 0
                    onTimeSelected(LocalTime.of(hour, minute))
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}