package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.Project
import com.nof1.data.model.ProjectWithHypotheses
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Project data and UI state.
 */
class ProjectViewModel(
    private val repository: ProjectRepository,
    private val generationRepository: HypothesisGenerationRepository? = null
) : ViewModel() {
    
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()
    
    private val _isGeneratingHypotheses = MutableStateFlow(false)
    val isGeneratingHypotheses: StateFlow<Boolean> = _isGeneratingHypotheses.asStateFlow()
    
    private val _generationError = MutableStateFlow<String?>(null)
    val generationError: StateFlow<String?> = _generationError.asStateFlow()
    
    private val _generatedHypotheses = MutableStateFlow<List<Hypothesis>>(emptyList())
    val generatedHypotheses: StateFlow<List<Hypothesis>> = _generatedHypotheses.asStateFlow()
    
    private val _apiCallDescription = MutableStateFlow<String?>(null)
    val apiCallDescription: StateFlow<String?> = _apiCallDescription.asStateFlow()
    
    val projects = repository.getActiveProjects()
    val allProjects = repository.getAllProjects()
    val projectsWithHypotheses = repository.getActiveProjectsWithHypotheses()
    val allProjectsWithHypotheses = repository.getAllProjectsWithHypotheses()
    
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }
    
    fun insertProject(project: Project) {
        viewModelScope.launch {
            val projectId = repository.insertProject(project)
            val savedProject = project.copy(id = projectId)
            
            // Automatically generate hypotheses if generation repository is available
            generationRepository?.let { genRepo ->
                _isGeneratingHypotheses.value = true
                _generationError.value = null
                _apiCallDescription.value = "Calling OpenAI API with prompt: \"Generate hypotheses for achieving ${savedProject.goal}, described as ${savedProject.description}\""
                
                genRepo.generateAndSaveHypotheses(savedProject)
                    .onSuccess { hypotheses ->
                        // Hypotheses successfully generated and saved
                        _generatedHypotheses.value = hypotheses
                        _isGeneratingHypotheses.value = false
                    }
                    .onFailure { error ->
                        _generationError.value = error.message ?: "Failed to generate hypotheses"
                        _isGeneratingHypotheses.value = false
                    }
            }
        }
    }
    
    fun updateProject(project: Project) {
        viewModelScope.launch {
            repository.updateProject(project)
        }
    }
    
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
        }
    }
    
    fun archiveProject(project: Project) {
        viewModelScope.launch {
            repository.archiveProject(project)
        }
    }
}

class ProjectViewModelFactory(
    private val repository: ProjectRepository,
    private val generationRepository: HypothesisGenerationRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(repository, generationRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 