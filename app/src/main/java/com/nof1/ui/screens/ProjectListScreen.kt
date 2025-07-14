package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
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
import com.nof1.viewmodel.ProjectViewModel
import com.nof1.viewmodel.ProjectViewModelFactory

/**
 * Screen displaying the list of projects.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectListScreen(
    onNavigateToProject: (String) -> Unit,
    onNavigateToAddProject: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val viewModel: ProjectViewModel = viewModel(
        factory = ProjectViewModelFactory(
            application.projectRepository,
            null // No hypothesis generation in list screen
        )
    )
    
    val showArchived by viewModel.showArchived.collectAsState()
    val projects by if (showArchived) {
        viewModel.allProjects.collectAsState(initial = emptyList())
    } else {
        viewModel.projects.collectAsState(initial = emptyList())
    }
    
    // TODO: Re-implement sync status tracking for Firebase-only version
    val isSyncing = false
    val syncError: String? = null
    
    // Log project count for debugging
    LaunchedEffect(projects) {
        android.util.Log.d("ProjectListScreen", "Projects updated: count=${projects.size}")
        projects.forEachIndexed { index, project ->
            android.util.Log.d("ProjectListScreen", "Project $index: ${project.name} (id=${project.id})")
        }
    }
    
    // TODO: Re-implement authentication state tracking for Firebase-only version
    val isAuthenticated = true // Assume authenticated for now
    val currentUserId = "firebase_user" // TODO: Get from AuthManager

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.projects)) },
                actions = {
                    // Manual sync button for debugging
                    IconButton(
                        onClick = { 
                            android.util.Log.d("ProjectListScreen", "Manual sync button clicked")
                            // TODO: Re-implement cloud sync for Firebase-only version
                            // viewModel.syncFromCloud() 
                        },
                        enabled = !isSyncing
                    ) {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Sync from Cloud"
                        )
                    }
                    IconButton(onClick = { viewModel.toggleShowArchived() }) {
                        Icon(
                            Icons.Default.Archive,
                            contentDescription = stringResource(R.string.show_archived)
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings)
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Show sync status and error messages
            if (isSyncing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Syncing from cloud...",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            syncError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = "Sync Error: $error",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Debug info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Debug Info:",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "Authenticated: $isAuthenticated",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "User ID: ${currentUserId ?: "None"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Projects Count: ${projects.size}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            if (projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.no_projects),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { 
                                android.util.Log.d("ProjectListScreen", "Manual sync from empty state")
                                // TODO: Re-implement cloud sync for Firebase-only version
                                // viewModel.syncFromCloud() 
                            }
                        ) {
                            Text("Try Sync From Cloud")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize(),
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
} 