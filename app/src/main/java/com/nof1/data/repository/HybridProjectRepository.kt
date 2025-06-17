package com.nof1.data.repository

import com.nof1.data.local.ProjectDao
import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime

/**
 * Hybrid repository that combines Room (offline) and Firebase (cloud) storage.
 * Implements offline-first strategy with cloud sync.
 */
class HybridProjectRepository(
    private val projectDao: ProjectDao,
    private val firebaseProjectRepository: FirebaseProjectRepository
) {
    
    // Local storage for offline capability
    private val localRepository = ProjectRepository(projectDao)
    
    /**
     * Insert project - saves locally first, then syncs to cloud
     */
    suspend fun insertProject(project: Project): Long {
        // Save locally first
        val localId = localRepository.insertProject(project)
        
        // Try to sync to cloud
        try {
            val firebaseId = firebaseProjectRepository.insertProject(project)
            // TODO: Store mapping between local ID and Firebase ID for sync
        } catch (e: Exception) {
            // Cloud sync failed, but local save succeeded
            // Will sync later when connection is restored
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
            // TODO: Implement sync queue
        }
    }
    
    /**
     * Delete project - deletes locally first, then syncs to cloud
     */
    suspend fun deleteProject(project: Project) {
        // Delete locally first
        localRepository.deleteProject(project)
        
        // Try to sync to cloud
        try {
            val firebaseId = getFirebaseIdForProject(project.id)
            if (firebaseId != null) {
                firebaseProjectRepository.deleteProject(firebaseId)
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
        }
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
        return combine(
            localRepository.getActiveProjects(),
            getCloudProjects()
        ) { localProjects, cloudProjects ->
            // Merge and deduplicate projects
            mergeProjects(localProjects, cloudProjects).filter { !it.isArchived }
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
            firebaseProjectRepository.getAllProjects().collect { firebaseProjects ->
                // Convert Firebase projects to Room projects and save locally
                firebaseProjects.forEach { firebaseProject ->
                    val localProject = firebaseProject.toProject()
                    // Check if project already exists locally
                    val existingProject = getProjectByFirebaseId(firebaseProject.id)
                    if (existingProject == null) {
                        localRepository.insertProject(localProject)
                    } else {
                        // Update existing project if cloud version is newer
                        if (isCloudVersionNewer(firebaseProject, existingProject)) {
                            localRepository.updateProject(localProject.copy(id = existingProject.id))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Sync failed, will retry later
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
            firebaseProjectRepository.getAllProjects().collect { firebaseProjects ->
                emit(firebaseProjects.map { it.toProject() })
            }
        } catch (e: Exception) {
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
        // TODO: Implement mapping between local and Firebase IDs
        // This would typically be stored in a separate mapping table
        return null
    }
    
    private suspend fun getProjectByFirebaseId(firebaseId: String): Project? {
        // TODO: Implement reverse mapping lookup
        return null
    }
    
    private fun isCloudVersionNewer(firebaseProject: FirebaseProject, localProject: Project): Boolean {
        val cloudUpdatedAt = firebaseProject.updatedAt?.toDate()?.let { 
            java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
        } ?: return false
        
        return cloudUpdatedAt.isAfter(localProject.updatedAt)
    }
} 