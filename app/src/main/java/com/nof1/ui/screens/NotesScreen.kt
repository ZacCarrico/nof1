package com.nof1.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
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
import com.nof1.data.model.Note
import com.nof1.viewmodel.NoteViewModel
import com.nof1.viewmodel.NoteViewModelFactory
import java.time.format.DateTimeFormatter

/**
 * Screen for managing notes for a hypothesis.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    hypothesisId: Long,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as Nof1Application
    val repository = application.noteRepository
    
    val viewModel: NoteViewModel = viewModel(
        factory = NoteViewModelFactory(repository)
    )
    
    val notes by viewModel.getNotesForHypothesis(hypothesisId).collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add note")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (notes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.no_notes_added),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Button(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Note")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(notes) { note ->
                    NoteCard(
                        note = note,
                        onEdit = { editingNote = note },
                        onDelete = { noteToDelete = note }
                    )
                }
            }
        }
    }
    
    // Add/Edit Note Dialog
    if (showAddDialog || editingNote != null) {
        NoteDialog(
            note = editingNote,
            onDismiss = {
                showAddDialog = false
                editingNote = null
            },
            onSave = { content ->
                if (editingNote != null) {
                    viewModel.updateNote(editingNote!!.copy(content = content))
                } else {
                    viewModel.insertNote(
                        Note(
                            hypothesisId = hypothesisId,
                            content = content
                        )
                    )
                }
                showAddDialog = false
                editingNote = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            title = { Text("Delete Note") },
            text = { Text("Are you sure you want to delete this note?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteNote(note)
                        noteToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text(stringResource(R.string.no))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteCard(
    note: Note,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = note.createdAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyLarge
            )
            
            if (note.updatedAt != note.createdAt) {
                Text(
                    text = "Updated: ${note.updatedAt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm"))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteDialog(
    note: Note?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var content by remember { mutableStateOf(note?.content ?: "") }
    
    // Full-screen overlay that takes up the entire screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (note != null) "Edit Note" else "Add Note",
                            maxLines = 1
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { onSave(content.trim()) },
                            enabled = content.trim().isNotBlank()
                        ) {
                            Text("Save")
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
                // Instructions for the user
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "💡 Hypothesis Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Use this space to capture your thoughts, observations, questions, and insights about your hypothesis. Consider noting:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "• Key observations and patterns\n• Questions that arise\n• Potential variables to consider\n• Insights from your experiments\n• Next steps and ideas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Large text area for extensive note-taking
                OutlinedTextField(
                    value = content,
                    onValueChange = { newValue -> content = newValue },
                    label = { Text("Your notes and thoughts") },
                    placeholder = { 
                        Text(
                            "Start writing your thoughts, observations, questions, and insights here...\n\nYou have plenty of space to capture detailed notes about your hypothesis and related experiments."
                        ) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Takes up all remaining space
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.4
                    ),
                    maxLines = Int.MAX_VALUE, // Allow unlimited lines for extensive note-taking
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${content.length} characters",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (note != null) {
                                Text(
                                    text = "Last updated: ${note.updatedAt.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )
            }
        }
    }
} 