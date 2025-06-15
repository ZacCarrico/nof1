package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Note
import com.nof1.data.repository.NoteRepository
import com.nof1.utils.ImageUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Note data and UI state.
 */
class NoteViewModel(
    private val repository: NoteRepository
) : ViewModel() {
    
    fun insertNote(note: Note) {
        viewModelScope.launch {
            repository.insertNote(note)
        }
    }
    
    fun updateNote(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note)
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            // Clean up image file if it exists
            note.imagePath?.let { imagePath ->
                ImageUtils.deleteImage(imagePath)
            }
            repository.deleteNote(note)
        }
    }
    
    fun getNotesForHypothesis(hypothesisId: Long): Flow<List<Note>> {
        return repository.getNotesForHypothesis(hypothesisId)
    }
}

class NoteViewModelFactory(
    private val repository: NoteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NoteViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NoteViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 