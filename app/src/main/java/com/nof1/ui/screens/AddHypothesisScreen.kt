package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
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
import com.nof1.data.model.Project
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.utils.SecureStorage
import com.nof1.viewmodel.HypothesisViewModel
import com.nof1.viewmodel.HypothesisViewModelFactory

/**
 * Screen for adding a new hypothesis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHypothesisScreen(
    projectId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.hypothesisRepository
    val projectRepository = application.projectRepository
    
    val secureStorage = remember { SecureStorage(context) }
    val generationRepository = remember { 
        if (secureStorage.hasOpenAIApiKey() || secureStorage.getApiBaseUrl().equals("test", ignoreCase = true)) {
            HypothesisGenerationRepository(secureStorage, repository)
        } else null
    }
    
    val viewModel: HypothesisViewModel = viewModel(
        factory = HypothesisViewModelFactory(repository, generationRepository)
    )
    
    var project by remember { mutableStateOf<Project?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    
    val generatedHypotheses by viewModel.generatedHypotheses.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    
    LaunchedEffect(projectId) {
        project = projectRepository.getProjectById(projectId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_hypothesis)) },
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
                            descriptionError = description.isBlank()
                            
                            if (!nameError && !descriptionError) {
                                val hypothesis = Hypothesis(
                                    projectId = projectId,
                                    name = name.trim(),
                                    description = description.trim()
                                )
                                viewModel.insertHypothesis(hypothesis)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { newValue: String ->
                        name = newValue
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.hypothesis_name)) },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { newValue: String ->
                        description = newValue
                        descriptionError = false
                    },
                    label = { Text(stringResource(R.string.hypothesis_description)) },
                    isError = descriptionError,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )
            }
            
            if (project != null) {
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
                                    "AI-Generated Suggestions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                if (generationRepository != null) {
                                    Button(
                                        onClick = { 
                                            viewModel.generateHypotheses(project!!)
                                        },
                                        enabled = !isGenerating
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
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isGenerating) "Generating..." else "Generate")
                                    }
                                }
                            }
                            
                            if (generationRepository == null) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = "To enable AI-powered hypothesis generation, please configure your OpenAI API key in Settings.",
                                        modifier = Modifier.padding(12.dp),
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
                                        text = error,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }
                
                if (generationRepository != null) {
                    items(generatedHypotheses) { hypothesis ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                if (name.isBlank()) {
                                    name = hypothesis.take(50) + if (hypothesis.length > 50) "..." else ""
                                }
                                description = hypothesis
                            }
                        ) {
                            Text(
                                text = hypothesis,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}