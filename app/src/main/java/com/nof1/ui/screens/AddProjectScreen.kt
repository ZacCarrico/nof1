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
import com.nof1.data.model.Project
import com.nof1.data.repository.ProjectRepository
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
    val viewModel: ProjectViewModel = viewModel(
        factory = ProjectViewModelFactory(repository)
    )
    
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf("") }
    
    var nameError by remember { mutableStateOf(false) }
    var descriptionError by remember { mutableStateOf(false) }
    var goalError by remember { mutableStateOf(false) }

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
                            descriptionError = description.isBlank()
                            goalError = goal.isBlank()
                            
                            if (!nameError && !descriptionError && !goalError) {
                                val project = Project(
                                    name = name.trim(),
                                    description = description.trim(),
                                    goal = goal.trim()
                                )
                                viewModel.insertProject(project)
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
                label = { Text(stringResource(R.string.project_name)) },
                isError = nameError,
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { newValue: String ->
                    description = newValue
                    descriptionError = false
                },
                label = { Text(stringResource(R.string.project_description)) },
                isError = descriptionError,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 5
            )
            
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
    }
} 