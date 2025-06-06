package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
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
import com.nof1.data.model.Hypothesis
import com.nof1.viewmodel.HypothesisViewModel
import com.nof1.viewmodel.HypothesisViewModelFactory
import java.time.format.DateTimeFormatter

/**
 * Screen for viewing and editing hypothesis details.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HypothesisDetailScreen(
    hypothesisId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToNotes: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.hypothesisRepository
    val noteRepository = application.noteRepository
    
    val viewModel: HypothesisViewModel = viewModel(
        factory = HypothesisViewModelFactory(repository)
    )
    
    var hypothesis by remember { mutableStateOf<Hypothesis?>(null) }
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }
    var editedDescription by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    
    // Load hypothesis data
    LaunchedEffect(hypothesisId) {
        hypothesis = repository.getHypothesisById(hypothesisId)
        hypothesis?.let {
            editedName = it.name
            editedDescription = it.description
        }
    }
    
    // Load notes data
    val notes by noteRepository.getNotesForHypothesis(hypothesisId).collectAsState(initial = emptyList())
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isEditing) stringResource(R.string.edit_hypothesis) else hypothesis?.name ?: "Hypothesis",
                        maxLines = 1
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (hypothesis != null) {
                        if (isEditing) {
                            TextButton(
                                onClick = {
                                    // Validate inputs
                                    nameError = editedName.isBlank()
                                    descriptionError = editedDescription.isBlank()
                                    
                                    if (!nameError && !descriptionError) {
                                        val updatedHypothesis = hypothesis!!.copy(
                                            name = editedName.trim(),
                                            description = editedDescription.trim()
                                        )
                                        viewModel.updateHypothesis(updatedHypothesis)
                                        hypothesis = updatedHypothesis
                                        isEditing = false
                                    }
                                }
                            ) {
                                Text(stringResource(R.string.save))
                            }
                            TextButton(
                                onClick = {
                                    // Cancel editing - reset to original values
                                    editedName = hypothesis!!.name
                                    editedDescription = hypothesis!!.description
                                    nameError = false
                                    descriptionError = false
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
        }
    ) { paddingValues ->
        if (hypothesis == null) {
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isEditing) {
                    // Edit mode - show text fields
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { newValue: String ->
                            editedName = newValue
                            nameError = false
                        },
                        label = { Text(stringResource(R.string.hypothesis_name)) },
                        isError = nameError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { newValue: String ->
                            editedDescription = newValue
                            descriptionError = false
                        },
                        label = { Text(stringResource(R.string.hypothesis_description)) },
                        isError = descriptionError,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 5
                    )
                } else {
                    // View mode - show text content
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Name",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = hypothesis!!.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                    
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Description",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = hypothesis!!.description,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                    
                    Card(
                        onClick = { onNavigateToNotes(hypothesisId) },
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
                                    text = stringResource(R.string.hypothesis_notes),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${notes.size} ${if (notes.size == 1) "note" else "notes"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            if (notes.isNotEmpty()) {
                                Text(
                                    text = notes.first().content,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Latest: ${notes.first().createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    text = stringResource(R.string.no_notes_added),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Tap to add your first note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    // Metadata card
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
                                    text = hypothesis!!.createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
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
                                    text = hypothesis!!.updatedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            if (hypothesis!!.isArchived) {
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