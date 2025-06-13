package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.nof1.data.model.Project
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.data.repository.ProjectRepository
import com.nof1.utils.SecureStorage
import com.nof1.viewmodel.ProjectViewModel
import com.nof1.viewmodel.ProjectViewModelFactory

/**
 * Screen for adding a new project.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProjectScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.projectRepository
    val hypothesisRepository = application.hypothesisRepository
    
    val secureStorage = remember { SecureStorage(context) }
    val generationRepository = remember { 
        if (secureStorage.hasOpenAIApiKey() || secureStorage.getApiBaseUrl().equals("test", ignoreCase = true)) {
            HypothesisGenerationRepository(secureStorage, hypothesisRepository)
        } else null
    }
    
    val viewModel: ProjectViewModel = viewModel(
        factory = ProjectViewModelFactory(repository, generationRepository)
    )
    
    // UI state variables
    var name by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var goalError by remember { mutableStateOf(false) }
    
    // Generation state
    val apiCallDescription by viewModel.apiCallDescription.collectAsState()
    val isGeneratingHypotheses by viewModel.isGeneratingHypotheses.collectAsState()
    val generatedHypotheses by viewModel.generatedHypotheses.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    var selectedHypotheses by remember { mutableStateOf(setOf<Int>()) }
    
    var projectSaved by remember { mutableStateOf(false) }
    
    // Save action
    val saveProject = {
        nameError = name.isBlank()
        goalError = goal.isBlank()
        
        if (!nameError && !goalError) {
            val project = Project(
                name = name.trim(),
                goal = goal.trim(),
            )
            viewModel.insertProject(project)
            projectSaved = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_project)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            // Validate inputs
                            nameError = name.isBlank()
                            goalError = goal.isBlank()
                            
                            if (!nameError && !goalError) {
                                val project = Project(
                                    name = name.trim(),
                                    goal = goal.trim(),
                                )
                                viewModel.insertProject(project)
                                projectSaved = true
                            }
                        },
                        enabled = !isGeneratingHypotheses && !projectSaved
                    ) {
                        if (isGeneratingHypotheses) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generating...")
                        } else if (projectSaved && generatedHypotheses.isNotEmpty()) {
                            Text("Done")
                        } else if (projectSaved) {
                            Text("Saved")
                        } else {
                            Text(stringResource(R.string.save))
                        }
                    }
                    
                    if (projectSaved && !isGeneratingHypotheses) {
                        TextButton(onClick = onNavigateBack) {
                            Text("Back to Projects")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!projectSaved) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { newValue: String ->
                            name = newValue
                            nameError = false
                        },
                        label = { Text(stringResource(R.string.project_name)) },
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                item {
                    OutlinedTextField(
                        value = goal,
                        onValueChange = { newValue: String ->
                            goal = newValue
                            goalError = false
                        },
                        label = { Text(stringResource(R.string.project_goal)) },
                        isError = goalError,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
                
                if (generationRepository != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "AI Hypothesis Generation",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = "Hypotheses will be automatically generated after saving your project.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Project Created Successfully!",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "Name: ${name.trim()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Text(
                                text = "Goal: ${goal.trim()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                
                if (generationRepository != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "API Call to Language Model",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                apiCallDescription?.let { description ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        )
                                    ) {
                                        Text(
                                            text = description,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                        )
                                    }
                                }
                                
                                if (isGeneratingHypotheses) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            text = "Calling API and generating hypotheses...",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                
                                generationError?.let { error ->
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = "Error: $error",
                                            modifier = Modifier.padding(12.dp),
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    if (generatedHypotheses.isNotEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Generated Hypotheses",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                
                                Text(
                                    text = "Click hypotheses to select those to keep",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveSelectedHypotheses(selectedHypotheses)
                                            onNavigateBack()
                                        },
                                        enabled = selectedHypotheses.isNotEmpty()
                                    ) {
                                        Text("Save Selected")
                                    }
                                    
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.saveAllHypotheses()
                                            onNavigateBack()
                                        }
                                    ) {
                                        Text("Accept All")
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
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = hypothesis.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                    
                                    Text(
                                        text = hypothesis.description,
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
                    }
                }
            }
        }
    }
} 