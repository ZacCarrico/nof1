package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.Project
import com.nof1.data.repository.HypothesisRepositoryInterface
import com.nof1.data.repository.HypothesisGenerationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Hypothesis data and UI state.
 */
class HypothesisViewModel(
    private val repository: HypothesisRepositoryInterface,
    private val generationRepository: HypothesisGenerationRepository? = null
) : ViewModel() {
    
    private val _generatedHypotheses = MutableStateFlow<List<String>>(emptyList())
    val generatedHypotheses: StateFlow<List<String>> = _generatedHypotheses.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()
    
    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()
    
    fun insertHypothesis(hypothesis: Hypothesis) {
        viewModelScope.launch {
            repository.insertHypothesis(hypothesis)
        }
    }
    
    fun updateHypothesis(hypothesis: Hypothesis) {
        viewModelScope.launch {
            repository.updateHypothesis(hypothesis)
        }
    }
    
    fun deleteHypothesis(hypothesis: Hypothesis) {
        viewModelScope.launch {
            repository.deleteHypothesis(hypothesis)
        }
    }
    
    fun archiveHypothesis(hypothesis: Hypothesis) {
        viewModelScope.launch {
            repository.archiveHypothesis(hypothesis)
        }
    }
    
    fun generateHypotheses(project: Project) {
        if (generationRepository == null) {
            _generationError.value = "Hypothesis generation not configured"
            return
        }
        
        viewModelScope.launch {
            try {
                _isGenerating.value = true
                _generationError.value = null
                
                generationRepository.generateHypothesesStrings(project)
                    .onSuccess { hypotheses ->
                        _generatedHypotheses.value = hypotheses
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
    
    fun clearGeneratedHypotheses() {
        _generatedHypotheses.value = emptyList()
        _generationError.value = null
    }
}

class HypothesisViewModelFactory(
    private val repository: HypothesisRepositoryInterface,
    private val generationRepository: HypothesisGenerationRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HypothesisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HypothesisViewModel(repository, generationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 