package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nof1.Nof1Application
import com.nof1.R
import com.nof1.data.model.Project
import com.nof1.ui.components.ProjectCard
import com.nof1.viewmodel.SimpleProjectViewModel

/**
 * Screen displaying the list of projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onNavigateToProject: (Long) -> Unit,
    onNavigateToAddProject: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val viewModel = remember { SimpleProjectViewModel(application.projectRepository) }
    
    val showArchived by viewModel.showArchived.collectAsState()
    val projects by if (showArchived) {
        viewModel.allProjects.collectAsState(initial = emptyList())
    } else {
        viewModel.projects.collectAsState(initial = emptyList())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects)) },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowArchived() }) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = stringResource(R.string.show_archived)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddProject
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_project))
            }
        }
    ) { paddingValues ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_projects),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onNavigateToProject(project.id) },
                        onArchive = { viewModel.archiveProject(project) },
                        onDelete = { viewModel.deleteProject(project) }
                    )
                }
            }
        }
    }
} 