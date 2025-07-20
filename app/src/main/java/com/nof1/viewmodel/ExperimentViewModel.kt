package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Experiment
import com.nof1.data.repository.ExperimentRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Experiment data and UI state.
 */
class ExperimentViewModel(
    private val repository: ExperimentRepository
) : ViewModel() {
    
    fun insertExperiment(experiment: Experiment) {
        viewModelScope.launch {
            repository.insertExperiment(experiment)
        }
    }
    
    fun updateExperiment(experiment: Experiment) {
        viewModelScope.launch {
            repository.updateExperiment(experiment)
        }
    }
    
    fun deleteExperiment(experiment: Experiment) {
        viewModelScope.launch {
            repository.deleteExperiment(experiment)
        }
    }
    
    fun archiveExperiment(experiment: Experiment) {
        viewModelScope.launch {
            repository.archiveExperiment(experiment)
        }
    }
    
    fun getExperimentsForHypothesis(hypothesisId: String) = 
        repository.getActiveExperimentsForHypothesis(hypothesisId)
}

class ExperimentViewModelFactory(
    private val repository: ExperimentRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExperimentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExperimentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}