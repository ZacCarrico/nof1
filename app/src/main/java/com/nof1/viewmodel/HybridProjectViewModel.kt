package com.nof1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.Project
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.repository.HybridProjectRepository
import com.nof1.data.repository.HypothesisGenerationRepository
import com.nof1.data.repository.ReminderRepository
import com.nof1.utils.AuthManager
import com.nof1.utils.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest

/**
 * Updated ViewModel for managing Project data with Firebase integration.
 * Uses HybridProjectRepository for offline-first with cloud sync.
 */
class HybridProjectViewModel(
    private val hybridRepository: HybridProjectRepository,
    private val generationRepository: HypothesisGenerationRepository? = null,
    private val authManager: AuthManager,
    private val context: Context? = null,
    private val reminderRepository: ReminderRepository? = null
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
    
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()
    
    // Data flows - now using hybrid repository
    val projects = hybridRepository.getActiveProjects()
    val allProjects = hybridRepository.getAllProjects()
    
    // Authentication state
    val isAuthenticated = authManager.isAuthenticated
    val currentUserId = authManager.currentUserId
    
    init {
        // Listen to authentication state changes and sync accordingly
        viewModelScope.launch {
            authManager.authStateFlow().collectLatest { user ->
                android.util.Log.d("HybridProjectViewModel", "Auth state changed: user=${user?.uid ?: "null"}")
                if (user != null) {
                    // User authenticated, start syncing
                    android.util.Log.d("HybridProjectViewModel", "User authenticated, starting sync from cloud")
                    syncFromCloud()
                } else {
                    // User signed out, clear sync state
                    android.util.Log.d("HybridProjectViewModel", "User signed out, clearing sync state")
                    _isSyncing.value = false
                    _syncError.value = null
                }
            }
        }
    }
    
    fun toggleShowArchived() {
        _showArchived.value = !_showArchived.value
    }
    
    private var _savedProject: Project? = null
    
    fun insertProject(project: Project) {
        viewModelScope.launch {
            try {
                val projectId = hybridRepository.insertProject(project)
                val savedProject = project.copy(id = projectId)
                _savedProject = savedProject
                
                // Generate hypotheses but don't auto-save them
                generationRepository?.let { genRepo ->
                    _isGeneratingHypotheses.value = true
                    _generationError.value = null
                    _apiCallDescription.value = "Calling OpenAI API with prompt: \"Generate hypotheses for achieving ${savedProject.goal}\""
                    
                    genRepo.generateHypotheses(savedProject)
                        .onSuccess { hypotheses ->
                            _generatedHypotheses.value = hypotheses
                            _isGeneratingHypotheses.value = false
                        }
                        .onFailure { error ->
                            _generationError.value = error.message ?: "Failed to generate hypotheses"
                            _isGeneratingHypotheses.value = false
                        }
                }
            } catch (e: Exception) {
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
            try {
                hybridRepository.updateProject(project)
            } catch (e: Exception) {
                _syncError.value = "Failed to update project: ${e.message}"
            }
        }
    }
    
    fun deleteProject(project: Project) {
        viewModelScope.launch {
            try {
                // Cancel all scheduled reminders and notifications before deletion
                cancelProjectNotifications(project.id)
                
                // Delete the project (includes comprehensive cleanup)
                hybridRepository.deleteProject(project)
            } catch (e: Exception) {
                _syncError.value = "Failed to delete project: ${e.message}"
            }
        }
    }
    
    fun archiveProject(project: Project) {
        viewModelScope.launch {
            try {
                hybridRepository.archiveProject(project)
            } catch (e: Exception) {
                _syncError.value = "Failed to archive project: ${e.message}"
            }
        }
    }
    
    /**
     * Manually trigger sync from cloud
     */
    fun syncFromCloud() {
        if (!authManager.isAuthenticated) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                hybridRepository.syncFromCloud()
            } catch (e: Exception) {
                _syncError.value = "Sync failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Manually trigger sync to cloud
     */
    fun syncToCloud() {
        if (!authManager.isAuthenticated) return
        
        viewModelScope.launch {
            _isSyncing.value = true
            _syncError.value = null
            
            try {
                hybridRepository.syncToCloud()
            } catch (e: Exception) {
                _syncError.value = "Upload failed: ${e.message}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
    
    /**
     * Clear sync error message
     */
    fun clearSyncError() {
        _syncError.value = null
    }
    
    /**
     * Sign out user
     */
    fun signOut() {
        authManager.signOut()
    }

    /**
     * Cancel all scheduled notifications for a project and its children
     */
    private suspend fun cancelProjectNotifications(projectId: Long) {
        context?.let { ctx ->
            reminderRepository?.let { repo ->
                try {
                    // Get project with all hypotheses for comprehensive cleanup
                    val projectWithHypotheses = hybridRepository.getProjectWithHypotheses(projectId).first()
                    
                    // Cancel project reminders
                    val projectReminders = repo.getReminderSettingsForEntitySync(ReminderEntityType.PROJECT, projectId)
                    projectReminders.forEach { reminder ->
                        ReminderScheduler.cancelReminder(ctx, reminder.id)
                    }
                    
                    // Cancel hypothesis reminders
                    projectWithHypotheses?.hypotheses?.forEach { hypothesis ->
                        val hypothesisReminders = repo.getReminderSettingsForEntitySync(ReminderEntityType.HYPOTHESIS, hypothesis.id)
                        hypothesisReminders.forEach { reminder ->
                            ReminderScheduler.cancelReminder(ctx, reminder.id)
                        }
                    }
                    
                    // TODO: Cancel experiment notifications
                    // This would require access to ExperimentRepository and WorkManager
                    // For now, we log this as a TODO since the repository pattern makes
                    // it challenging to access all required dependencies here
                    android.util.Log.d("HybridProjectViewModel", "TODO: Cancel experiment notifications for project $projectId")
                    
                } catch (e: Exception) {
                    android.util.Log.e("HybridProjectViewModel", "Failed to cancel notifications for project $projectId: ${e.message}")
                }
            }
        }
    }
}

class HybridProjectViewModelFactory(
    private val hybridRepository: HybridProjectRepository,
    private val generationRepository: HypothesisGenerationRepository? = null,
    private val authManager: AuthManager,
    private val context: Context? = null,
    private val reminderRepository: ReminderRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HybridProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HybridProjectViewModel(hybridRepository, generationRepository, authManager, context, reminderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 