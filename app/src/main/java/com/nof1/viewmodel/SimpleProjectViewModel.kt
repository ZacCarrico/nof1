package com.nof1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Project
import com.nof1.data.repository.ProjectRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Simplified ViewModel for projects.
 * Updated to work with Firebase-only repositories.
 */
class SimpleProjectViewModel(
    private val repository: ProjectRepository
) : ViewModel() {
    
    private val _showArchived = MutableStateFlow(false)
    val showArchived: StateFlow<Boolean> = _showArchived.asStateFlow()
    
    val projects = repository.getActiveProjects()
    val allProjects = repository.getAllProjects()
    
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
    
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }
    
    fun getProjectById(projectId: String) = repository.getProjectWithHypotheses(projectId)
} 