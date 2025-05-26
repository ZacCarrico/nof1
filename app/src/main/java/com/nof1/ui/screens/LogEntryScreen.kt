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
import com.nof1.data.model.LogEntry
import com.nof1.data.repository.ExperimentRepository
import com.nof1.data.repository.LogEntryRepository
import com.nof1.viewmodel.LogEntryViewModel
import com.nof1.viewmodel.LogEntryViewModelFactory

/**
 * Screen for quickly logging an experiment response.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEntryScreen(
    experimentId: Long,
    isFromNotification: Boolean = false,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val logEntryRepository = LogEntryRepository(application.database.logEntryDao())
    val experimentRepository = ExperimentRepository(application.database.experimentDao())
    
    val viewModel: LogEntryViewModel = viewModel(
        factory = LogEntryViewModelFactory(logEntryRepository, experimentRepository)
    )
    
    val experiment by experimentRepository.getExperimentWithLogs(experimentId)
        .collectAsState(initial = null)
    
    var response by remember { mutableStateOf("") }
    var responseError by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.log_entry)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            responseError = response.isBlank()
                            
                            if (!responseError) {
                                val logEntry = LogEntry(
                                    experimentId = experimentId,
                                    response = response.trim(),
                                    isFromNotification = isFromNotification
                                )
                                viewModel.insertLogEntry(logEntry)
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
            // Experiment info
            experiment?.experiment?.let { exp ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = exp.name,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        
                        if (exp.description.isNotBlank()) {
                            Text(
                                text = exp.description,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Text(
                            text = exp.question,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
            
            // Response input
            OutlinedTextField(
                value = response,
                onValueChange = { newValue: String ->
                    response = newValue
                    responseError = false
                },
                label = { Text(stringResource(R.string.log_response)) },
                placeholder = { Text("Enter your response...") },
                isError = responseError,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 6
            )
            
            if (isFromNotification) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "📱 Responding to notification",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
} 