package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nof1.Nof1Application
import com.nof1.R
import com.nof1.data.repository.HypothesisRepository
import com.nof1.data.repository.ProjectRepository
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderSettings
import com.nof1.ui.components.HypothesisCard
import com.nof1.ui.components.ReminderSettingsCard
import com.nof1.ui.components.ReminderDialog
import com.nof1.viewmodel.HypothesisViewModel
import com.nof1.viewmodel.HypothesisViewModelFactory
import com.nof1.viewmodel.ReminderViewModel
import com.nof1.viewmodel.ReminderViewModelFactory

/**
 * Screen displaying the details of a project and its hypotheses.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    projectId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToHypothesis: (Long) -> Unit,
    onNavigateToAddHypothesis: (Long) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val projectRepository = application.projectRepository
    val hypothesisRepository = application.hypothesisRepository
    val reminderRepository = application.reminderRepository
    
    val hypothesisViewModel: HypothesisViewModel = viewModel(
        factory = HypothesisViewModelFactory(hypothesisRepository)
    )
    
    val reminderViewModel: ReminderViewModel = viewModel(
        factory = ReminderViewModelFactory(reminderRepository, context)
    )
    
    val project by projectRepository.getProjectWithHypotheses(projectId)
        .collectAsState(initial = null)
    
    val hypotheses by hypothesisRepository.getActiveHypothesesForProject(projectId)
        .collectAsState(initial = emptyList())
    
    val projectReminders by reminderViewModel.getReminderSettingsForEntity(
        ReminderEntityType.PROJECT, projectId
    ).collectAsState(initial = emptyList())
    
    var showReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderSettings?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = project?.project?.name ?: "Project",
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToAddHypothesis(projectId) }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_hypothesis))
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Project info section
                item {
                    project?.project?.let { proj ->
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = proj.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                
                                if (proj.goal.isNotBlank()) {
                                    Text(
                                        text = "Goal: ${proj.goal}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Reminder settings section
                item {
                    ReminderSettingsCard(
                        reminders = projectReminders,
                        onAddReminder = {
                            editingReminder = null
                            showReminderDialog = true
                        },
                        onEditReminder = { reminder ->
                            editingReminder = reminder
                            showReminderDialog = true
                        },
                        onDeleteReminder = { reminder ->
                            reminderViewModel.deleteReminder(reminder)
                        },
                        onToggleReminder = { reminder, isEnabled ->
                            reminderViewModel.toggleReminderEnabled(reminder.id, isEnabled)
                        }
                    )
                }
                
                // Hypotheses section header
                item {
                    Text(
                        text = "Hypotheses",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                // Hypotheses content
                if (hypotheses.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.no_hypotheses),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                } else {
                    items(hypotheses) { hypothesis ->
                        HypothesisCard(
                            hypothesis = hypothesis,
                            onClick = { onNavigateToHypothesis(hypothesis.id) },
                            onArchive = { hypothesisViewModel.archiveHypothesis(hypothesis) },
                            onDelete = { hypothesisViewModel.deleteHypothesis(hypothesis) }
                        )
                    }
                }
            }
        }
    }
    
    // Reminder dialog
    if (showReminderDialog) {
        ReminderDialog(
            isEdit = editingReminder != null,
            initialReminder = editingReminder,
            entityType = ReminderEntityType.PROJECT,
            entityId = projectId,
            onDismiss = {
                showReminderDialog = false
                editingReminder = null
            },
            onSave = { reminder ->
                if (editingReminder != null) {
                    reminderViewModel.updateReminder(reminder)
                } else {
                    reminderViewModel.createReminder(reminder)
                }
                showReminderDialog = false
                editingReminder = null
            }
        )
    }
} 