package com.nof1.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
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
import com.nof1.data.repository.HypothesisRepository
import com.nof1.data.repository.ProjectRepository
import com.nof1.ui.components.HypothesisCard
import com.nof1.viewmodel.HypothesisViewModel
import com.nof1.viewmodel.HypothesisViewModelFactory

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
    val projectRepository = ProjectRepository(application.database.projectDao())
    val hypothesisRepository = HypothesisRepository(application.database.hypothesisDao())
    
    val hypothesisViewModel: HypothesisViewModel = viewModel(
        factory = HypothesisViewModelFactory(hypothesisRepository)
    )
    
    val project by projectRepository.getProjectWithHypotheses(projectId)
        .collectAsState(initial = null)
    
    val hypotheses by hypothesisRepository.getActiveHypothesesForProject(projectId)
        .collectAsState(initial = emptyList())

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
            // Project info section
            project?.project?.let { proj ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = proj.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
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
            
            // Hypotheses section
            if (hypotheses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_hypotheses),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
} 