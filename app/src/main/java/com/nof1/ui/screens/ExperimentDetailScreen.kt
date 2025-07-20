package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nof1.Nof1Application
import com.nof1.R
import com.nof1.data.model.Experiment
import com.nof1.data.model.LogEntry
import com.nof1.data.model.ReminderSettings
import com.nof1.ui.components.ReminderSettingsCard
import com.nof1.viewmodel.ExperimentViewModel
import com.nof1.viewmodel.ExperimentViewModelFactory
import com.nof1.viewmodel.ReminderViewModel
import com.nof1.viewmodel.ReminderViewModelFactory
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Screen for viewing and managing experiment details, reminders, and logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExperimentDetailScreen(
    experimentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToAddLog: (String) -> Unit = {},
    onNavigateToAddReminder: (String) -> Unit = {},
    onNavigateToEditReminder: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val experimentRepository = application.experimentRepository
    val logEntryRepository = application.logEntryRepository
    val reminderRepository = application.reminderRepository
    val hypothesisRepository = application.hypothesisRepository
    
    val experimentViewModel: ExperimentViewModel = viewModel(
        factory = ExperimentViewModelFactory(experimentRepository)
    )
    
    val reminderViewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(application, reminderRepository)
    )
    
    var experiment by remember { mutableStateOf<Experiment?>(null) }
    var hypothesisName by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    var editedQuestion by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    
    // Load experiment data
    LaunchedEffect(experimentId) {
        experiment = experimentRepository.getExperimentById(experimentId)
        experiment?.let { exp ->
            editedName = exp.name
            editedDescription = exp.description
            editedQuestion = exp.question
            
            // Load hypothesis name
            val hypothesis = hypothesisRepository.getHypothesisById(exp.hypothesisId)
            hypothesisName = hypothesis?.name ?: ""
        }
    }
    
    // Load logs data
    val logs by logEntryRepository.getLogEntriesForExperiment(experimentId).collectAsState(initial = emptyList())
    
    // Load reminders data
    val reminders by reminderRepository.getReminderSettingsForEntity("EXPERIMENT", experimentId).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditing) stringResource(R.string.edit_experiment) else experiment?.name ?: "Experiment",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (experiment != null) {
                        if (isEditing) {
                            TextButton(
                                onClick = {
                                    // Validate inputs
                                    nameError = editedName.isBlank()
                                    
                                    if (!nameError) {
                                        val updatedExperiment = experiment!!.copy(
                                            name = editedName.trim(),
                                            description = editedDescription.trim(),
                                            question = editedQuestion.trim()
                                        )
                                        experimentViewModel.updateExperiment(updatedExperiment)
                                        experiment = updatedExperiment
                                        isEditing = false
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.save))
                            }
                            TextButton(
                                onClick = {
                                    // Cancel editing - reset to original values
                                    editedName = experiment!!.name
                                    editedDescription = experiment!!.description
                                    editedQuestion = experiment!!.question
                                    nameError = false
                                    isEditing = false
                                }
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                        } else {
                            IconButton(
                                onClick = { isEditing = true }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddLog(experimentId) }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Log Entry")
            }
        }
    ) { paddingValues ->
        if (experiment == null) {
            // Loading state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (isEditing) {
                        // Edit mode - show text fields
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                OutlinedTextField(
                                    value = editedName,
                                    onValueChange = { newValue: String ->
                                        editedName = newValue
                                        nameError = false
                                    },
                                    label = { Text(stringResource(R.string.experiment_name)) },
                                    isError = nameError,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                OutlinedTextField(
                                    value = editedDescription,
                                    onValueChange = { newValue: String ->
                                        editedDescription = newValue
                                    },
                                    label = { Text(stringResource(R.string.experiment_description) + " (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                                
                                OutlinedTextField(
                                    value = editedQuestion,
                                    onValueChange = { newValue: String ->
                                        editedQuestion = newValue
                                    },
                                    label = { Text(stringResource(R.string.experiment_question) + " (Optional)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 3
                                )
                            }
                        }
                    } else {
                        // View mode - show experiment details
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Experiment Details",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Text(
                                    text = experiment!!.name,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                
                                if (experiment!!.description.isNotBlank()) {
                                    Text(
                                        text = experiment!!.description,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                                
                                if (experiment!!.question.isNotBlank()) {
                                    Text(
                                        text = "Question: ${experiment!!.question}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                if (hypothesisName.isNotBlank()) {
                                    Text(
                                        text = "For Hypothesis: $hypothesisName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Reminders section
                item {
                    ReminderSettingsCard(
                        reminders = reminders,
                        onAddReminder = { onNavigateToAddReminder(experimentId) },
                        onEditReminder = { reminder -> onNavigateToEditReminder(reminder.id) },
                        onDeleteReminder = { reminder -> reminderViewModel.deleteReminder(reminder) },
                        onToggleReminder = { reminder, enabled -> 
                            reminderViewModel.updateReminder(reminder.copy(isEnabled = enabled))
                        }
                    )
                }
                
                // Logs section
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Log Entries",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${logs.size} ${if (logs.size == 1) "entry" else "entries"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            if (logs.isEmpty()) {
                                Text(
                                    text = "No log entries yet. Tap the + button to add your first entry.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Show recent log entries
                items(logs.take(5)) { log ->
                    LogEntryItem(log = log)
                }
                
                if (logs.size > 5) {
                    item {
                        TextButton(
                            onClick = { /* TODO: Navigate to all logs */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View All ${logs.size} Entries")
                        }
                    }
                }
                
                // Metadata card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Created:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = experiment!!.getCreatedAtAsLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Updated:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = experiment!!.getUpdatedAtAsLocalDateTime().format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            if (experiment!!.archived) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Status:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Archived",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogEntryItem(
    log: LogEntry,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = log.response,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = LocalDateTime.ofInstant(log.createdAt?.toDate()?.toInstant() ?: Instant.now(), ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}