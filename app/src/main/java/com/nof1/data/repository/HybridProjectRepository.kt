package com.nof1.data.repository

import com.nof1.data.local.ProjectDao
import com.nof1.data.local.LogEntryDao
import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

/**
 * Hybrid repository that combines Room (offline) and Firebase (cloud) storage.
 * Implements offline-first strategy with cloud sync.
 */
class HybridProjectRepository(
    private val projectDao: ProjectDao,
    private val firebaseProjectRepository: FirebaseProjectRepository,
    private val mappingRepository: FirebaseMappingRepository,
    private val firebaseHypothesisRepository: FirebaseHypothesisRepository,
    private val firebaseExperimentRepository: FirebaseExperimentRepository,
    private val firebaseLogEntryRepository: FirebaseLogEntryRepository,
    private val logEntryDao: LogEntryDao
) {
    
    // Local storage for offline capability
    private val localRepository = ProjectRepository(projectDao)
    
    // Hypothesis repository for syncing hypotheses when projects are synced
    private var hybridHypothesisRepository: HybridHypothesisRepository? = null
    
    // Additional repositories for cleanup operations
    private var reminderRepository: ReminderRepository? = null
    private var noteRepository: NoteRepository? = null
    
    /**
     * Set the hypothesis repository after initialization to avoid circular dependency
     */
    fun setHypothesisRepository(hypothesisRepository: HybridHypothesisRepository) {
        this.hybridHypothesisRepository = hypothesisRepository
    }
    
    /**
     * Set additional repositories needed for cleanup operations
     */
    fun setCleanupRepositories(
        reminderRepo: ReminderRepository,
        noteRepo: NoteRepository
    ) {
        this.reminderRepository = reminderRepo
        this.noteRepository = noteRepo
    }
    
    /**
     * Insert project - saves locally first, then syncs to cloud
     */
    suspend fun insertProject(project: Project): Long {
        // Save locally first
        val localId = localRepository.insertProject(project)
        
        // Try to sync to cloud
        try {
            val firebaseId = firebaseProjectRepository.insertProject(project)
            if (firebaseId != null) {
                // Store mapping between local ID and Firebase ID
                mappingRepository.storeMapping(
                    FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                    localId,
                    firebaseId
                )
            }
        } catch (e: Exception) {
            // Cloud sync failed, but local save succeeded
            // Will sync later when connection is restored
            android.util.Log.e("HybridProjectRepository", "Failed to sync project to cloud: ${e.message}")
        }
        
        return localId
    }
    
    /**
     * Update project - updates locally first, then syncs to cloud
     */
    suspend fun updateProject(project: Project) {
        val updatedProject = project.copy(updatedAt = LocalDateTime.now())
        
        // Update locally first
        localRepository.updateProject(updatedProject)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForProject(project.id)
            if (firebaseId != null) {
                firebaseProjectRepository.updateProject(firebaseId, updatedProject)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
            android.util.Log.e("HybridProjectRepository", "Failed to update project in cloud: ${e.message}")
            // TODO: Implement sync queue
        }
    }
    
    /**
     * Delete project with comprehensive cleanup - deletes locally first, then syncs to cloud
     */
    suspend fun deleteProject(project: Project) {
        // Get all related data before deletion for comprehensive cleanup
        val projectWithHypotheses = projectDao.getProjectWithHypotheses(project.id).first()
        val firebaseId = getFirebaseIdForProject(project.id)
        
        // 1. Cancel all scheduled reminders and notifications
        cancelProjectReminders(project.id)
        projectWithHypotheses?.hypotheses?.forEach { hypothesis ->
            cancelHypothesisReminders(hypothesis.id)
            cancelExperimentNotifications(hypothesis.id)
        }
        
        // 2. Clean up notes and their image files
        projectWithHypotheses?.hypotheses?.forEach { hypothesis ->
            cleanupNotesForHypothesis(hypothesis.id)
        }
        
        // 3. Delete from Firebase first (manual cascading delete)
        try {
            if (firebaseId != null) {
                deleteProjectFromFirebaseWithCascade(firebaseId, projectWithHypotheses?.hypotheses ?: emptyList())
            }
        } catch (e: Exception) {
            android.util.Log.e("HybridProjectRepository", "Failed to delete project from Firebase: ${e.message}")
        }
        
        // 4. Clean up Firebase mappings for all child entities
        cleanupFirebaseMappings(project.id, projectWithHypotheses?.hypotheses ?: emptyList())
        
        // 5. Delete locally last (CASCADE will handle hypotheses, experiments, log entries)
        localRepository.deleteProject(project)
    }
    
    /**
     * Archive project - archives locally first, then syncs to cloud
     */
    suspend fun archiveProject(project: Project) {
        // Archive locally first
        localRepository.archiveProject(project)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForProject(project.id)
            if (firebaseId != null) {
                firebaseProjectRepository.archiveProject(firebaseId, project)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
        }
    }
    
    /**
     * Get project by ID - returns local data with cloud sync in background
     */
    suspend fun getProjectById(id: Long): Project? {
        return localRepository.getProjectById(id)
    }
    
    /**
     * Get active projects - combines local and cloud data
     */
    fun getActiveProjects(): Flow<List<Project>> {
        android.util.Log.d("HybridProjectRepository", "getActiveProjects() called")
        return combine(
            localRepository.getActiveProjects(),
            getCloudProjects()
        ) { localProjects, cloudProjects ->
            android.util.Log.d("HybridProjectRepository", "Combining data: local=${localProjects.size}, cloud=${cloudProjects.size}")
            // Merge and deduplicate projects
            val merged = mergeProjects(localProjects, cloudProjects).filter { !it.isArchived }
            android.util.Log.d("HybridProjectRepository", "Active projects result: ${merged.size}")
            merged
        }
    }
    
    /**
     * Get archived projects
     */
    fun getArchivedProjects(): Flow<List<Project>> {
        return combine(
            localRepository.getArchivedProjects(),
            getCloudProjects()
        ) { localProjects, cloudProjects ->
            mergeProjects(localProjects, cloudProjects).filter { it.isArchived }
        }
    }
    
    /**
     * Get all projects
     */
    fun getAllProjects(): Flow<List<Project>> {
        return combine(
            localRepository.getAllProjects(),
            getCloudProjects()
        ) { localProjects, cloudProjects ->
            mergeProjects(localProjects, cloudProjects)
        }
    }
    
    /**
     * Get project with hypotheses
     */
    fun getProjectWithHypotheses(projectId: Long): Flow<ProjectWithHypotheses?> {
        return localRepository.getProjectWithHypotheses(projectId)
    }

    /**
     * Get project with hypotheses and experiments
     */
    fun getProjectWithHypothesesAndExperiments(projectId: Long): Flow<ProjectWithHypothesesAndExperiments?> {
        return localRepository.getProjectWithHypothesesAndExperiments(projectId)
    }
    
    /**
     * Sync all data from cloud to local storage
     */
    suspend fun syncFromCloud() {
        try {
            android.util.Log.d("HybridProjectRepository", "Starting sync from cloud")
            // Use first() to get one-time snapshot instead of continuous listening
            val firebaseProjects = firebaseProjectRepository.getAllProjects().first()
            android.util.Log.d("HybridProjectRepository", "Syncing ${firebaseProjects.size} projects from cloud")
            
            // Convert Firebase projects to Room projects and save locally
            firebaseProjects.forEach { firebaseProject ->
                val localProject = firebaseProject.toProject()
                android.util.Log.d("HybridProjectRepository", "Processing Firebase project: ${firebaseProject.name} (${firebaseProject.id})")
                
                // Check if project already exists locally (either by mapping or by name as fallback)
                val existingProject = getProjectByFirebaseId(firebaseProject.id)
                android.util.Log.d("HybridProjectRepository", "Existing project from mapping: $existingProject")
                
                val existingByName = if (existingProject == null) {
                    localRepository.getAllProjects().first().find { it.name == localProject.name && it.goal == localProject.goal }
                } else null
                android.util.Log.d("HybridProjectRepository", "Existing project by name: $existingByName")
                
                val finalExistingProject = existingProject ?: existingByName
                
                if (finalExistingProject == null) {
                    android.util.Log.d("HybridProjectRepository", "Inserting new project from cloud: ${localProject.name}")
                    val localId = localRepository.insertProject(localProject)
                    
                    // Store mapping between Firebase ID and local ID
                    android.util.Log.d("HybridProjectRepository", "Storing mapping: local=$localId, firebase=${firebaseProject.id}")
                    mappingRepository.storeMapping(
                        FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                        localId,
                        firebaseProject.id
                    )
                    
                    // Sync hypotheses for this project
                    hybridHypothesisRepository?.let { hypothesisRepo ->
                        android.util.Log.d("HybridProjectRepository", "Syncing hypotheses for project: ${firebaseProject.id}")
                        try {
                            hypothesisRepo.syncFromCloud(firebaseProject.id, localId)
                        } catch (e: Exception) {
                            android.util.Log.e("HybridProjectRepository", "Failed to sync hypotheses for project ${firebaseProject.id}: ${e.message}")
                        }
                    }
                } else {
                    android.util.Log.d("HybridProjectRepository", "Project already exists locally: ${finalExistingProject.name} (${finalExistingProject.id})")
                    
                    // Ensure mapping exists for this project  
                    val existingMapping = mappingRepository.getFirebaseId(
                        FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                        finalExistingProject.id
                    )
                    if (existingMapping == null) {
                        android.util.Log.d("HybridProjectRepository", "Creating missing mapping for existing project")
                        mappingRepository.storeMapping(
                            FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                            finalExistingProject.id,
                            firebaseProject.id
                        )
                    }
                    
                    // Update existing project if cloud version is newer
                    if (isCloudVersionNewer(firebaseProject, finalExistingProject)) {
                        android.util.Log.d("HybridProjectRepository", "Updating existing project from cloud: ${localProject.name}")
                        localRepository.updateProject(localProject.copy(id = finalExistingProject.id))
                    }
                }
            }
            android.util.Log.d("HybridProjectRepository", "Cloud sync completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("HybridProjectRepository", "Cloud sync failed: ${e.message}", e)
        }
    }
    
    /**
     * Sync all local data to cloud
     */
    suspend fun syncToCloud() {
        try {
            localRepository.getAllProjects().collect { localProjects ->
                localProjects.forEach { project ->
                    val firebaseId = getFirebaseIdForProject(project.id)
                    if (firebaseId == null) {
                        // New project, insert to cloud
                        firebaseProjectRepository.insertProject(project)
                    } else {
                        // Existing project, update in cloud
                        firebaseProjectRepository.updateProject(firebaseId, project)
                    }
                }
            }
        } catch (e: Exception) {
            // Sync failed, will retry later
        }
    }
    
    // Helper functions
    
    private fun getCloudProjects(): Flow<List<Project>> = flow {
        try {
            android.util.Log.d("HybridProjectRepository", "Starting cloud projects collection")
            firebaseProjectRepository.getAllProjects().collect { firebaseProjects ->
                android.util.Log.d("HybridProjectRepository", "Received ${firebaseProjects.size} projects from Firebase")
                val localProjects = firebaseProjects.map { it.toProject() }
                emit(localProjects)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            android.util.Log.d("HybridProjectRepository", "Cloud projects collection cancelled")
            // Don't log as error - this is expected when scope is cancelled
            throw e
        } catch (e: Exception) {
            android.util.Log.e("HybridProjectRepository", "Error getting cloud projects: ${e.message}", e)
            emit(emptyList())
        }
    }
    
    private fun mergeProjects(localProjects: List<Project>, cloudProjects: List<Project>): List<Project> {
        // Simple merge strategy - prefer local data, add cloud-only items
        val mergedMap = mutableMapOf<String, Project>()
        
        // Add local projects first
        localProjects.forEach { project ->
            mergedMap[project.name] = project // Use name as key for simplicity
        }
        
        // Add cloud projects that don't exist locally
        cloudProjects.forEach { cloudProject ->
            if (!mergedMap.containsKey(cloudProject.name)) {
                mergedMap[cloudProject.name] = cloudProject
            }
        }
        
        return mergedMap.values.sortedByDescending { it.createdAt }
    }
    
    private suspend fun getFirebaseIdForProject(localId: Long): String? {
        return mappingRepository.getFirebaseId(
            FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
            localId
        )
    }
    
    private suspend fun getProjectByFirebaseId(firebaseId: String): Project? {
        val localId = mappingRepository.getLocalId(
            FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
            firebaseId
        ) ?: return null
        
        return localRepository.getProjectById(localId)
    }
    
    private fun isCloudVersionNewer(firebaseProject: FirebaseProject, localProject: Project): Boolean {
        val cloudUpdatedAt = firebaseProject.updatedAt?.toDate()?.let { 
            java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
        } ?: return false
        
        return cloudUpdatedAt.isAfter(localProject.updatedAt)
    }
    
    /**
     * Cancel reminder notifications for a project
     */
    private suspend fun cancelProjectReminders(projectId: Long) {
        reminderRepository?.let { repo ->
            val reminders = repo.getReminderSettingsForEntitySync(ReminderEntityType.PROJECT, projectId)
            reminders.forEach { reminder ->
                // Cancel scheduled notifications for this reminder
                // Note: ReminderScheduler.cancelReminder() should be called from the UI layer with context
                repo.deleteReminderSettings(reminder)
            }
        }
    }
    
    /**
     * Cancel reminder notifications for a hypothesis
     */
    private suspend fun cancelHypothesisReminders(hypothesisId: Long) {
        reminderRepository?.let { repo ->
            val reminders = repo.getReminderSettingsForEntitySync(ReminderEntityType.HYPOTHESIS, hypothesisId)
            reminders.forEach { reminder ->
                // Cancel scheduled notifications for this reminder
                // Note: ReminderScheduler.cancelReminder() should be called from the UI layer with context
                repo.deleteReminderSettings(reminder)
            }
        }
    }
    
    /**
     * Cancel experiment notifications for all experiments in a hypothesis
     */
    private suspend fun cancelExperimentNotifications(hypothesisId: Long) {
        // Get all experiments for this hypothesis and cancel their notifications
        // Note: ExperimentNotificationWorker cancellation should be handled at the UI layer
        // This is a placeholder for that logic
        android.util.Log.d("HybridProjectRepository", "TODO: Cancel experiment notifications for hypothesis $hypothesisId")
    }
    
    /**
     * Clean up notes and their associated image files for a hypothesis
     */
    private suspend fun cleanupNotesForHypothesis(hypothesisId: Long) {
        noteRepository?.deleteAllNotesForHypothesis(hypothesisId)
    }
    
    /**
     * Delete project from Firebase with manual cascading delete
     */
    private suspend fun deleteProjectFromFirebaseWithCascade(firebaseProjectId: String, hypotheses: List<Hypothesis>) {
        android.util.Log.d("HybridProjectRepository", "Starting Firebase cascading delete for project $firebaseProjectId")
        
        // For each hypothesis, delete all its experiments and log entries
        hypotheses.forEach { hypothesis ->
            val firebaseHypothesisId = mappingRepository.getFirebaseId(
                FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                hypothesis.id
            )
            
            if (firebaseHypothesisId != null) {
                // Get all experiments for this hypothesis to delete their log entries
                val hypothesisWithExperiments = projectDao.getProjectWithHypothesesAndExperiments(hypothesis.projectId).first()
                val experiments = hypothesisWithExperiments?.hypothesesWithExperiments
                    ?.find { it.hypothesis.id == hypothesis.id }?.experiments ?: emptyList()
                
                // Delete all log entries for each experiment
                experiments.forEach { experiment ->
                    val firebaseExperimentId = mappingRepository.getFirebaseId(
                        FirebaseMappingRepository.ENTITY_TYPE_EXPERIMENT,
                        experiment.id
                    )
                    
                    if (firebaseExperimentId != null) {
                        try {
                            // Delete all log entries for this experiment from Firebase
                            val logEntries = logEntryDao.getLogEntriesForExperimentSync(experiment.id)
                            logEntries.forEach { logEntry ->
                                val firebaseLogEntryId = mappingRepository.getFirebaseId(
                                    FirebaseMappingRepository.ENTITY_TYPE_LOG_ENTRY,
                                    logEntry.id
                                )
                                if (firebaseLogEntryId != null) {
                                    try {
                                        firebaseLogEntryRepository.deleteLogEntry(firebaseLogEntryId)
                                        android.util.Log.d("HybridProjectRepository", "Deleted log entry $firebaseLogEntryId from Firebase")
                                    } catch (e: Exception) {
                                        android.util.Log.e("HybridProjectRepository", "Failed to delete log entry $firebaseLogEntryId: ${e.message}")
                                    }
                                }
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("HybridProjectRepository", "Failed to delete log entries for experiment $firebaseExperimentId: ${e.message}")
                        }
                        
                        // Delete the experiment from Firebase
                        try {
                            firebaseExperimentRepository.deleteExperiment(firebaseExperimentId)
                            android.util.Log.d("HybridProjectRepository", "Deleted experiment $firebaseExperimentId from Firebase")
                        } catch (e: Exception) {
                            android.util.Log.e("HybridProjectRepository", "Failed to delete experiment $firebaseExperimentId: ${e.message}")
                        }
                    }
                }
                
                // Delete the hypothesis from Firebase
                try {
                    firebaseHypothesisRepository.deleteHypothesis(firebaseHypothesisId)
                    android.util.Log.d("HybridProjectRepository", "Deleted hypothesis $firebaseHypothesisId from Firebase")
                } catch (e: Exception) {
                    android.util.Log.e("HybridProjectRepository", "Failed to delete hypothesis $firebaseHypothesisId: ${e.message}")
                }
            }
        }
        
        // Finally, delete the project from Firebase
        try {
            firebaseProjectRepository.deleteProject(firebaseProjectId)
            android.util.Log.d("HybridProjectRepository", "Deleted project $firebaseProjectId from Firebase")
        } catch (e: Exception) {
            android.util.Log.e("HybridProjectRepository", "Failed to delete project $firebaseProjectId: ${e.message}")
            throw e
        }
    }
    
    /**
     * Clean up Firebase mappings for all child entities
     */
    private suspend fun cleanupFirebaseMappings(projectId: Long, hypotheses: List<Hypothesis>) {
        // Clean up project mapping
        mappingRepository.deleteMappingByLocalId(
            FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
            projectId
        )
        
        hypotheses.forEach { hypothesis ->
            // Clean up hypothesis mapping
            mappingRepository.deleteMappingByLocalId(
                FirebaseMappingRepository.ENTITY_TYPE_HYPOTHESIS,
                hypothesis.id
            )
            
            // Get all experiments for this hypothesis to clean up their mappings
            try {
                val hypothesisWithExperiments = projectDao.getProjectWithHypothesesAndExperiments(projectId).first()
                val experiments = hypothesisWithExperiments?.hypothesesWithExperiments
                    ?.find { it.hypothesis.id == hypothesis.id }?.experiments ?: emptyList()
                
                experiments.forEach { experiment ->
                    // Clean up experiment mapping
                    mappingRepository.deleteMappingByLocalId(
                        FirebaseMappingRepository.ENTITY_TYPE_EXPERIMENT,
                        experiment.id
                    )
                    
                    // Clean up log entry mappings for this experiment
                    try {
                        val logEntries = logEntryDao.getLogEntriesForExperimentSync(experiment.id)
                        logEntries.forEach { logEntry ->
                            mappingRepository.deleteMappingByLocalId(
                                FirebaseMappingRepository.ENTITY_TYPE_LOG_ENTRY,
                                logEntry.id
                            )
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("HybridProjectRepository", "Failed to clean up log entry mappings for experiment ${experiment.id}: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HybridProjectRepository", "Failed to clean up mappings for hypothesis ${hypothesis.id}: ${e.message}")
            }
        }
    }
} 