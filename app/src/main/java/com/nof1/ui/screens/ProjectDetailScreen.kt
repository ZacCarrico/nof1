package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
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
import com.nof1.data.model.Hypothesis
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.data.repository.HypothesisRepository
import com.nof1.data.repository.ProjectRepository
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderSettings
import com.nof1.ui.components.HypothesisCard
import com.nof1.ui.components.ReminderSettingsCard
import com.nof1.ui.components.ReminderDialog
import com.nof1.utils.SecureStorage
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
    
    val secureStorage = remember { SecureStorage(context) }
    val generationRepository = remember { 
        if (secureStorage.hasOpenAIApiKey() || secureStorage.getApiBaseUrl().equals("test", ignoreCase = true)) {
            HypothesisGenerationRepository(secureStorage, hypothesisRepository)
        } else null
    }
    
    val hypothesisViewModel: HypothesisViewModel = viewModel(
        factory = HypothesisViewModelFactory(hypothesisRepository, generationRepository)
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
    
    // Hypothesis generation state
    val generatedHypotheses by hypothesisViewModel.generatedHypotheses.collectAsState()
    val isGenerating by hypothesisViewModel.isGenerating.collectAsState()
    val generationError by hypothesisViewModel.generationError.collectAsState()
    
    var showReminderDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<ReminderSettings?>(null) }
    var selectedHypotheses by remember { mutableStateOf(setOf<Int>()) }

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hypotheses",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        if (generationRepository != null && project?.project != null) {
                            Button(
                                onClick = { 
                                    hypothesisViewModel.generateHypotheses(project!!.project)
                                    selectedHypotheses = setOf() // Reset selection
                                },
                                enabled = !isGenerating,
                                modifier = Modifier.height(36.dp)
                            ) {
                                if (isGenerating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGenerating) "Generating..." else "Generate",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
                
                // Generation error display
                generationError?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                // Generated hypotheses selection UI
                if (generationRepository != null && generatedHypotheses.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Generated Hypotheses",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            
                            Text(
                                text = "Click hypotheses to select those to save",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val selectedList = selectedHypotheses.map { generatedHypotheses[it] }
                                        selectedList.forEachIndexed { index, hypothesis ->
                                            val hypothesisObj = Hypothesis(
                                                projectId = projectId,
                                                name = if (hypothesis.length > 50) {
                                                    hypothesis.take(47) + "..."
                                                } else {
                                                    hypothesis
                                                },
                                                description = hypothesis
                                            )
                                            hypothesisViewModel.insertHypothesis(hypothesisObj)
                                        }
                                        hypothesisViewModel.clearGeneratedHypotheses()
                                    },
                                    enabled = selectedHypotheses.isNotEmpty()
                                ) {
                                    Text("Save Selected")
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        generatedHypotheses.forEachIndexed { index, hypothesis ->
                                            val hypothesisObj = Hypothesis(
                                                projectId = projectId,
                                                name = if (hypothesis.length > 50) {
                                                    hypothesis.take(47) + "..."
                                                } else {
                                                    hypothesis
                                                },
                                                description = hypothesis
                                            )
                                            hypothesisViewModel.insertHypothesis(hypothesisObj)
                                        }
                                        hypothesisViewModel.clearGeneratedHypotheses()
                                    }
                                ) {
                                    Text("Accept All")
                                }
                                
                                TextButton(
                                    onClick = {
                                        hypothesisViewModel.clearGeneratedHypotheses()
                                    }
                                ) {
                                    Text("Dismiss")
                                }
                            }
                        }
                    }
                    
                    items(generatedHypotheses.size) { index ->
                        val hypothesis = generatedHypotheses[index]
                        val isSelected = selectedHypotheses.contains(index)
                        
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedHypotheses = if (isSelected) {
                                    selectedHypotheses - index
                                } else {
                                    selectedHypotheses + index
                                }
                            },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Text(
                                text = hypothesis,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                
                // API key configuration info for generation
                if (generationRepository == null && hypotheses.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "💡 Auto-Generate Hypotheses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "To enable AI-powered hypothesis generation for this project, please configure your OpenAI API key in Settings.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                // Existing hypotheses content
                if (hypotheses.isEmpty() && generatedHypotheses.isEmpty()) {
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