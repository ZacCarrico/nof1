package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Project
import com.nof1.data.model.ProjectWithHypotheses
import com.nof1.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Project data and UI state.
 */
class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()
    
    val projects = repository.getActiveProjects()
    val allProjects = repository.getAllProjects()
    val projectsWithHypotheses = repository.getActiveProjectsWithHypotheses()
    val allProjectsWithHypotheses = repository.getAllProjectsWithHypotheses()
    
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }
    
    fun insertProject(project: Project) {
        viewModelScope.launch {
            repository.insertProject(project)
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

class ProjectViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProjectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 