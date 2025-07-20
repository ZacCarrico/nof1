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
import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
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
    
    val viewModel: ExperimentViewModel = viewModel(
        factory = ExperimentViewModelFactory(repository)
    )
    
    var hypothesis by remember { mutableStateOf<Hypothesis?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var question by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    
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