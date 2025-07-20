package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-only repository for accessing Project data.
 * This replaces the hybrid repository pattern.
 */
class ProjectRepository : BaseFirebaseRepository() {
    
    private val projectsCollection = firestore.collection("projects")
    
    /**
     * Insert a new project
     */
    suspend fun insertProject(project: Project): String? {
        val userId = requireUserId()
        val firebaseProject = project.copy(
            userId = userId,
            createdAt = null, // Let Firebase set this with @ServerTimestamp
            updatedAt = null  // Let Firebase set this with @ServerTimestamp
        )
        android.util.Log.d("ProjectRepository", "Inserting project: name=${project.name}, userId=$userId")
        val result = addDocument(projectsCollection, firebaseProject)
        android.util.Log.d("ProjectRepository", "Insert result: $result")
        return result
    }
    
    /**
     * Update an existing project
     */
    suspend fun updateProject(project: Project): Boolean {
        val userId = requireUserId()
        val updatedProject = project.copy(userId = userId).copyWithUpdatedTimestamp()
        return updateDocument(projectsCollection, project.id, updatedProject)
    }
    
    /**
     * Delete a project
     */
    suspend fun deleteProject(project: Project): Boolean {
        return deleteDocument(projectsCollection, project.id)
    }
    
    /**
     * Archive a project
     */
    suspend fun archiveProject(project: Project): Boolean {
        val archivedProject = project.copy(archived = true).copyWithUpdatedTimestamp()
        return updateDocument(projectsCollection, project.id, archivedProject)
    }
    
    /**
     * Get project by ID
     */
    suspend fun getProjectById(projectId: String): Project? {
        return getDocumentById<Project>(projectsCollection, projectId)
    }
    
    /**
     * Get all active projects (non-archived)
     */
    fun getActiveProjects(): Flow<List<Project>> {
        val userId = requireUserId()
        android.util.Log.d("ProjectRepository", "Setting up active projects listener for userId: $userId")
        return getCollectionAsFlow<Project>(projectsCollection) { collection ->
            try {
                // Try with orderBy first
                collection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("archived", false)
                    .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            } catch (e: Exception) {
                android.util.Log.w("ProjectRepository", "OrderBy query failed, trying without orderBy: ${e.message}")
                // Fallback without orderBy in case of indexing issues
                collection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("archived", false)
            }
        }
    }
    
    /**
     * Get all archived projects
     */
    fun getArchivedProjects(): Flow<List<Project>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Project>(projectsCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", true)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get all projects
     */
    fun getAllProjects(): Flow<List<Project>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Project>(projectsCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get project with its hypotheses
     */
    fun getProjectWithHypotheses(projectId: String): Flow<ProjectWithHypotheses?> = flow {
        try {
            val project = getProjectById(projectId)
            if (project != null) {
                val hypotheses = getHypothesesForProject(projectId)
                emit(ProjectWithHypotheses(project, hypotheses))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    /**
     * Get project with hypotheses and experiments
     */
    fun getProjectWithHypothesesAndExperiments(projectId: String): Flow<ProjectWithHypothesesAndExperiments?> = flow {
        try {
            val project = getProjectById(projectId)
            if (project != null) {
                val hypothesesWithExperiments = getHypothesesWithExperimentsForProject(projectId)
                emit(ProjectWithHypothesesAndExperiments(project, hypothesesWithExperiments))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    /**
     * Get all active projects with their hypotheses
     */
    fun getActiveProjectsWithHypotheses(): Flow<List<ProjectWithHypotheses>> = flow {
        try {
            getActiveProjects().collect { projects ->
                val projectsWithHypotheses = projects.map { project ->
                    val hypotheses = getHypothesesForProject(project.id)
                    ProjectWithHypotheses(project, hypotheses)
                }
                emit(projectsWithHypotheses)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * Get all projects with their hypotheses
     */
    fun getAllProjectsWithHypotheses(): Flow<List<ProjectWithHypotheses>> = flow {
        try {
            getAllProjects().collect { projects ->
                val projectsWithHypotheses = projects.map { project ->
                    val hypotheses = getHypothesesForProject(project.id)
                    ProjectWithHypotheses(project, hypotheses)
                }
                emit(projectsWithHypotheses)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Helper methods
    private suspend fun getHypothesesForProject(projectId: String): List<Hypothesis> {
        val userId = requireUserId()
        return try {
            firestore.collection("hypotheses")
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Hypothesis::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private suspend fun getHypothesesWithExperimentsForProject(projectId: String): List<HypothesisWithExperiments> {
        val hypotheses = getHypothesesForProject(projectId)
        return hypotheses.map { hypothesis ->
            val experiments = getExperimentsForHypothesis(hypothesis.id)
            HypothesisWithExperiments(hypothesis, experiments)
        }
    }
    
    private suspend fun getExperimentsForHypothesis(hypothesisId: String): List<Experiment> {
        val userId = requireUserId()
        return try {
            firestore.collection("experiments")
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Experiment::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Direct query method for debugging - bypasses Flow
     */
    suspend fun testDirectQuery(): Int {
        val userId = requireUserId()
        android.util.Log.d("ProjectRepository", "Testing direct query with userId: $userId")
        
        return try {
            // Query all projects for this user
            val allProjectsSnapshot = projectsCollection
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            android.util.Log.d("ProjectRepository", "All projects query returned ${allProjectsSnapshot.documents.size} documents")
            allProjectsSnapshot.documents.forEach { doc ->
                android.util.Log.d("ProjectRepository", "Project doc ${doc.id}: ${doc.data}")
            }
            
            // Query active projects specifically (without orderBy)
            val activeProjectsSnapshot = projectsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .get()
                .await()
            
            android.util.Log.d("ProjectRepository", "Active projects query returned ${activeProjectsSnapshot.documents.size} documents")
            activeProjectsSnapshot.documents.forEach { doc ->
                android.util.Log.d("ProjectRepository", "Active project doc ${doc.id}: ${doc.data}")
            }
            
            // Test query with orderBy to see if indexing is the issue
            try {
                val orderedProjectsSnapshot = projectsCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("archived", false)
                    .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                android.util.Log.d("ProjectRepository", "Ordered projects query returned ${orderedProjectsSnapshot.documents.size} documents")
            } catch (e: Exception) {
                android.util.Log.e("ProjectRepository", "Ordered query failed - possible indexing issue: ${e.message}", e)
            }
            
            activeProjectsSnapshot.documents.size
        } catch (e: Exception) {
            android.util.Log.e("ProjectRepository", "Direct query failed: ${e.message}", e)
            -1
        }
    }
} 