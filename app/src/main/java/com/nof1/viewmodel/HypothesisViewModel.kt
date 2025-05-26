package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Hypothesis
import com.nof1.data.repository.HypothesisRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Hypothesis data and UI state.
 */
class HypothesisViewModel(private val repository: HypothesisRepository) : ViewModel() {
    
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
}

class HypothesisViewModelFactory(private val repository: HypothesisRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HypothesisViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HypothesisViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 