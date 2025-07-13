package com.nof1.data.repository

import com.nof1.data.local.ProjectDao
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
    private val mappingRepository: FirebaseMappingRepository
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
     * Delete project - deletes locally first, then syncs to cloud
     */
    suspend fun deleteProject(project: Project) {
        // Get Firebase ID before deleting locally
        val firebaseId = getFirebaseIdForProject(project.id)
        
        // Delete locally first
        localRepository.deleteProject(project)
        
        // Try to sync to cloud
        try {
            if (firebaseId != null) {
                firebaseProjectRepository.deleteProject(firebaseId)
                // Clean up mapping
                mappingRepository.deleteMappingByLocalId(
                    FirebaseMappingRepository.ENTITY_TYPE_PROJECT,
                    project.id
                )
            }
        } catch (e: Exception) {
            // Cloud sync failed, mark for later sync
            android.util.Log.e("HybridProjectRepository", "Failed to delete project from cloud: ${e.message}")
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
                // Check if project already exists locally
                val existingProject = getProjectByFirebaseId(firebaseProject.id)
                if (existingProject == null) {
                    android.util.Log.d("HybridProjectRepository", "Inserting new project from cloud: ${localProject.name}")
                    localRepository.insertProject(localProject)
                } else {
                    // Update existing project if cloud version is newer
                    if (isCloudVersionNewer(firebaseProject, existingProject)) {
                        android.util.Log.d("HybridProjectRepository", "Updating existing project from cloud: ${localProject.name}")
                        localRepository.updateProject(localProject.copy(id = existingProject.id))
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
} 