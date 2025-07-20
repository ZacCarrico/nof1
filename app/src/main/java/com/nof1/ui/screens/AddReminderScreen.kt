package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nof1.Nof1Application
import com.nof1.R
import com.nof1.data.model.ReminderSettings
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.ReminderEntityType
import com.nof1.viewmodel.ReminderViewModel
import com.nof1.viewmodel.ReminderViewModelFactory
import java.time.LocalTime

/**
 * Screen for adding a new reminder to an experiment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReminderScreen(
    experimentId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.reminderRepository
    val experimentRepository = application.experimentRepository
    
    val viewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(application, repository)
    )
    
    var experiment by remember { mutableStateOf<com.nof1.data.model.Experiment?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var selectedFrequency by remember { mutableStateOf(ReminderFrequency.DAILY) }
    
    var titleError by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    LaunchedEffect(experimentId) {
        experiment = experimentRepository.getExperimentById(experimentId)
        experiment?.let { exp ->
            title = "Reminder for ${exp.name}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Reminder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Validate inputs
                            titleError = title.isBlank()
                            
                            if (!titleError) {
                                val reminder = ReminderSettings(
                                    entityType = ReminderEntityType.EXPERIMENT.name,
                                    entityId = experimentId,
                                    projectId = experiment?.projectId ?: "",
                                    title = title.trim(),
                                    description = description.trim(),
                                    frequency = selectedFrequency.name,
                                    timeHour = selectedTime.hour,
                                    timeMinute = selectedTime.minute
                                )
                                viewModel.createReminder(reminder)
                                onNavigateBack()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            experiment?.let { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "For Experiment:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = exp.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
            
            OutlinedTextField(
                value = title,
                onValueChange = { newValue: String ->
                    title = newValue
                    titleError = false
                },
                label = { Text("Reminder Title") },
                isError = titleError,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { newValue: String ->
                    description = newValue
                },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Reminder Time",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("${selectedTime.hour.toString().padStart(2, '0')}:${selectedTime.minute.toString().padStart(2, '0')}")
                    }
                }
            }
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Frequency",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    ReminderFrequency.values().forEach { frequency ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            RadioButton(
                                selected = selectedFrequency == frequency,
                                onClick = { selectedFrequency = frequency }
                            )
                            Text(
                                text = frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showTimePicker) {
        TimePickerDialog(
            onTimeSelected = { hour, minute ->
                selectedTime = LocalTime.of(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
            initialTime = selectedTime
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    onTimeSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    initialTime: LocalTime
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Time") },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onTimeSelected(timePickerState.hour, timePickerState.minute)
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