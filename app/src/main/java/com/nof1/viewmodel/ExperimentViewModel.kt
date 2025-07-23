package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Experiment
import com.nof1.data.model.Hypothesis
import com.nof1.data.repository.ExperimentRepository
import com.nof1.data.repository.ExperimentGenerationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Experiment data and UI state.
 */
class ExperimentViewModel(
    private val repository: ExperimentRepository,
    private val generationRepository: ExperimentGenerationRepository? = null
) : ViewModel() {
    
    private val _generatedExperiments = MutableStateFlow<List<String>>(emptyList())
    val generatedExperiments: StateFlow<List<String>> = _generatedExperiments.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()
    
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
    
    fun generateExperiments(hypothesis: Hypothesis) {
        if (generationRepository == null) {
            _generationError.value = "Experiment generation not configured"
            return
        }
        
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                _generationError.value = null
                
                generationRepository.generateExperimentsStrings(hypothesis)
                    .onSuccess { experiments ->
                        _generatedExperiments.value = experiments
                        _isGenerating.value = false
                    }
                    .onFailure { error ->
                        _generationError.value = error.message ?: "Unknown error occurred"
                        _isGenerating.value = false
                    }
            } catch (e: Exception) {
                _generationError.value = "Unexpected error: ${e.message}"
                _isGenerating.value = false
            }
        }
    }
    
    fun clearGeneratedExperiments() {
        _generatedExperiments.value = emptyList()
        _generationError.value = null
    }
}

class ExperimentViewModelFactory(
    private val repository: ExperimentRepository,
    private val generationRepository: ExperimentGenerationRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExperimentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExperimentViewModel(repository, generationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}