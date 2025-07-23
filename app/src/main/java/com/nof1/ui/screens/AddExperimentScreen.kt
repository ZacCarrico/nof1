package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
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
import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
import com.nof1.data.repository.ExperimentGenerationRepository
import com.nof1.utils.SecureStorage
import com.nof1.viewmodel.ExperimentViewModel
import com.nof1.viewmodel.ExperimentViewModelFactory

/**
 * Screen for adding a new experiment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExperimentScreen(
    hypothesisId: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.experimentRepository
    val hypothesisRepository = application.hypothesisRepository
    
    val secureStorage = remember { SecureStorage(context) }
    val generationRepository = remember { 
        if (secureStorage.hasOpenAIApiKey() || secureStorage.getApiBaseUrl().equals("test", ignoreCase = true)) {
            ExperimentGenerationRepository(secureStorage, application.experimentRepository)
        } else null
    }
    
    val viewModel: ExperimentViewModel = viewModel(
        factory = ExperimentViewModelFactory(repository, generationRepository)
    )
    
    var hypothesis by remember { mutableStateOf<Hypothesis?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    
    val generatedExperiments by viewModel.generatedExperiments.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationError by viewModel.generationError.collectAsState()
    
    var selectedExperiments by remember { mutableStateOf(setOf<Int>()) }
    
    LaunchedEffect(hypothesisId) {
        hypothesis = hypothesisRepository.getHypothesisById(hypothesisId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_experiment)) },
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
                            
                            if (!nameError) {
                                val experiment = Experiment(
                                    hypothesisId = hypothesisId,
                                    projectId = hypothesis?.projectId ?: "",
                                    name = name.trim(),
                                    description = description.trim(),
                                    question = question.trim()
                                )
                                viewModel.insertExperiment(experiment)
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
            OutlinedTextField(
                value = name,
                onValueChange = { newValue: String ->
                    name = newValue
                    nameError = false
                },
                label = { Text(stringResource(R.string.experiment_name)) },
                isError = nameError,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { newValue: String ->
                    description = newValue
                },
                label = { Text(stringResource(R.string.experiment_description) + " (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            OutlinedTextField(
                value = question,
                onValueChange = { newValue: String ->
                    question = newValue
                },
                label = { Text(stringResource(R.string.experiment_question) + " (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            if (hypothesis != null) {
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
                                        viewModel.generateExperiments(hypothesis!!)
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
                                    text = "To enable AI-powered experiment generation, please configure your OpenAI API key in Settings.",
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
                
                if (generationRepository != null && generatedExperiments.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Click experiments to select those to keep",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val selectedList = selectedExperiments.map { generatedExperiments[it] }
                                    selectedList.forEachIndexed { index, experimentText ->
                                        val experiment = Experiment(
                                            hypothesisId = hypothesisId,
                                            projectId = hypothesis?.projectId ?: "",
                                            name = "Generated Experiment ${index + 1}",
                                            description = experimentText,
                                            question = ""
                                        )
                                        viewModel.insertExperiment(experiment)
                                    }
                                    onNavigateBack()
                                },
                                enabled = selectedExperiments.isNotEmpty()
                            ) {
                                Text("Save Selected")
                            }
                            
                            OutlinedButton(
                                onClick = {
                                    generatedExperiments.forEachIndexed { index, experimentText ->
                                        val experiment = Experiment(
                                            hypothesisId = hypothesisId,
                                            projectId = hypothesis?.projectId ?: "",
                                            name = "Generated Experiment ${index + 1}",
                                            description = experimentText,
                                            question = ""
                                        )
                                        viewModel.insertExperiment(experiment)
                                    }
                                    onNavigateBack()
                                }
                            ) {
                                Text("Accept All")
                            }
                        }
                        
                        generatedExperiments.forEachIndexed { index, experimentText ->
                            val isSelected = selectedExperiments.contains(index)
                            
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    selectedExperiments = if (isSelected) {
                                        selectedExperiments - index
                                    } else {
                                        selectedExperiments + index
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
                                    text = experimentText,
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
                }
            }
            
            hypothesis?.let { hyp ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "For Hypothesis:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = hyp.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (hyp.description.isNotBlank()) {
                            Text(
                                text = hyp.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}