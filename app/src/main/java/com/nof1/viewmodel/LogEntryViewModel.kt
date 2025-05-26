package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.LogEntry
import com.nof1.data.repository.ExperimentRepository
import com.nof1.data.repository.LogEntryRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing LogEntry data and UI state.
 */
class LogEntryViewModel(
    private val logEntryRepository: LogEntryRepository,
    private val experimentRepository: ExperimentRepository
) : ViewModel() {
    
    fun insertLogEntry(logEntry: LogEntry) {
        viewModelScope.launch {
            logEntryRepository.insertLogEntry(logEntry)
            // Update the experiment's last logged time
            experimentRepository.updateLastLoggedAt(logEntry.experimentId)
        }
    }
    
    fun updateLogEntry(logEntry: LogEntry) {
        viewModelScope.launch {
            logEntryRepository.updateLogEntry(logEntry)
        }
    }
    
    fun deleteLogEntry(logEntry: LogEntry) {
        viewModelScope.launch {
            logEntryRepository.deleteLogEntry(logEntry)
        }
    }
}

class LogEntryViewModelFactory(
    private val logEntryRepository: LogEntryRepository,
    private val experimentRepository: ExperimentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LogEntryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LogEntryViewModel(logEntryRepository, experimentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 