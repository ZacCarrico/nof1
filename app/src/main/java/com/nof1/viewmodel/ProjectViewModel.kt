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
 * Updated to work with Firebase-only repositories.
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
    
    // Add a refresh trigger to force reload projects
    private val _refreshTrigger = MutableStateFlow(0)
    fun refreshProjects() {
        _refreshTrigger.value += 1
        android.util.Log.d("ProjectViewModel", "Manual refresh triggered")
    }
    
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }
    
    private var _savedProject: Project? = null
    
    fun insertProject(project: Project) {
        viewModelScope.launch {
            try {
                android.util.Log.d("ProjectViewModel", "Inserting project: ${project.name}")
                val projectId = repository.insertProject(project)
                if (projectId != null) {
                    android.util.Log.d("ProjectViewModel", "Project inserted successfully with ID: $projectId")
                    val savedProject = project.copy(id = projectId)
                    _savedProject = savedProject
                    
                    // Trigger a refresh to ensure the new project appears in the list
                    refreshProjects()
                    
                    // Generate hypotheses but don't auto-save them
                    generationRepository?.let { genRepo ->
                        _isGeneratingHypotheses.value = true
                        _generationError.value = null
                        _apiCallDescription.value = "Calling OpenAI API with prompt: \"Generate hypotheses for achieving ${savedProject.goal}\""
                        
                        genRepo.generateHypotheses(savedProject)
                            .onSuccess { hypotheses ->
                                // Hypotheses generated but not saved yet
                                _generatedHypotheses.value = hypotheses
                                _isGeneratingHypotheses.value = false
                            }
                            .onFailure { error ->
                                _generationError.value = error.message ?: "Failed to generate hypotheses"
                                _isGeneratingHypotheses.value = false
                            }
                    }
                } else {
                    android.util.Log.e("ProjectViewModel", "Failed to insert project - projectId is null")
                    _generationError.value = "Failed to save project"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProjectViewModel", "Exception while inserting project: ${e.message}", e)
                _generationError.value = "Failed to save project: ${e.message}"
            }
        }
    }
    
    fun saveSelectedHypotheses(selectedIndices: Set<Int>) {
        viewModelScope.launch {
            _savedProject?.let { project ->
                val selectedHypotheses = selectedIndices.map { index ->
                    _generatedHypotheses.value[index].copy(projectId = project.id)
                }
                generationRepository?.saveHypotheses(selectedHypotheses)
            }
        }
    }
    
    fun saveAllHypotheses() {
        viewModelScope.launch {
            _savedProject?.let { project ->
                val allHypotheses = _generatedHypotheses.value.map { hypothesis ->
                    hypothesis.copy(projectId = project.id)
                }
                generationRepository?.saveHypotheses(allHypotheses)
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
    
    fun getProjectById(projectId: String) = repository.getProjectWithHypotheses(projectId)
    
    fun getProjectWithHypothesesAndExperiments(projectId: String) = 
        repository.getProjectWithHypothesesAndExperiments(projectId)
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