package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of Project repository.
 * Handles cloud storage for Projects using Firestore.
 */
class FirebaseProjectRepository : BaseFirebaseRepository() {
    
    private val projectsCollection = firestore.collection("projects")
    
    suspend fun insertProject(project: Project): String? {
        val userId = requireUserId()
        val firebaseProject = project.toFirebaseProject(userId)
        return addDocument(projectsCollection, firebaseProject)
    }
    
    suspend fun updateProject(firebaseProjectId: String, project: Project): Boolean {
        val userId = requireUserId()
        val firebaseProject = project.toFirebaseProject(userId, firebaseProjectId)
        return updateDocument(projectsCollection, firebaseProjectId, firebaseProject)
    }
    
    suspend fun deleteProject(firebaseProjectId: String): Boolean {
        return deleteDocument(projectsCollection, firebaseProjectId)
    }
    
    suspend fun archiveProject(firebaseProjectId: String, project: Project): Boolean {
        val userId = requireUserId()
        val archivedProject = project.copy(isArchived = true)
        val firebaseProject = archivedProject.toFirebaseProject(userId, firebaseProjectId)
        return updateDocument(projectsCollection, firebaseProjectId, firebaseProject)
    }
    
    suspend fun getProjectById(firebaseProjectId: String): FirebaseProject? {
        return getDocumentById<FirebaseProject>(projectsCollection, firebaseProjectId)
    }
    
    fun getActiveProjects(): Flow<List<FirebaseProject>> {
        return flow {
            try {
                val userId = requireUserId()
                android.util.Log.d("FirebaseProjectRepository", "Getting active projects for user: $userId")
                getCollectionAsFlow<FirebaseProject>(projectsCollection) { collection ->
                    collection
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("isArchived", false)
                        // Temporarily removing orderBy to test basic data loading
                        // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }.collect { projects ->
                    android.util.Log.d("FirebaseProjectRepository", "Firebase returned ${projects.size} active projects")
                    emit(projects)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseProjectRepository", "Error getting active projects: ${e.message}", e)
                emit(emptyList<FirebaseProject>())
            }
        }
    }
    
    fun getArchivedProjects(): Flow<List<FirebaseProject>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseProject>(projectsCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getAllProjects(): Flow<List<FirebaseProject>> {
        return flow {
            try {
                val userId = requireUserId()
                android.util.Log.d("FirebaseProjectRepository", "Getting all projects for user: $userId")
                getCollectionAsFlow<FirebaseProject>(projectsCollection) { collection ->
                    collection
                        .whereEqualTo("userId", userId)
                        // Temporarily removing orderBy to test basic data loading
                        // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }.collect { projects ->
                    android.util.Log.d("FirebaseProjectRepository", "Firebase returned ${projects.size} total projects")
                    emit(projects)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseProjectRepository", "Error getting all projects: ${e.message}", e)
                emit(emptyList<FirebaseProject>())
            }
        }
    }
    
    /**
     * Get project with all its hypotheses and experiments
     * This requires multiple Firestore queries due to NoSQL nature
     */
    fun getProjectWithHypothesesAndExperiments(firebaseProjectId: String): Flow<FirebaseProjectWithHypothesesAndExperiments?> = flow {
        try {
            val project = getProjectById(firebaseProjectId) ?: return@flow emit(null)
            
            // Get hypotheses for this project
            val hypothesesSnapshot = firestore.collection("hypotheses")
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", requireUserId())
                .get()
                .await()
            
            val hypothesesWithExperiments = hypothesesSnapshot.documents.mapNotNull { hypothesisDoc: com.google.firebase.firestore.DocumentSnapshot ->
                val hypothesis = hypothesisDoc.toObject(FirebaseHypothesis::class.java) ?: return@mapNotNull null
                
                // Get experiments for this hypothesis
                val experimentsSnapshot = firestore.collection("experiments")
                    .whereEqualTo("hypothesisId", hypothesis.id)
                    .whereEqualTo("userId", requireUserId())
                    .get()
                    .await()
                
                val experiments = experimentsSnapshot.documents.mapNotNull { experimentDoc: com.google.firebase.firestore.DocumentSnapshot ->
                    experimentDoc.toObject(FirebaseExperiment::class.java)
                }
                
                FirebaseHypothesisWithExperiments(hypothesis, experiments)
            }
            
            emit(FirebaseProjectWithHypothesesAndExperiments(project, hypothesesWithExperiments))
            
        } catch (e: Exception) {
            emit(null)
        }
    }
}

/**
 * Data classes for Firebase relations (similar to Room relations)
 */
data class FirebaseProjectWithHypothesesAndExperiments(
    val project: FirebaseProject,
    val hypotheses: List<FirebaseHypothesisWithExperiments>
)

data class FirebaseHypothesisWithExperiments(
    val hypothesis: FirebaseHypothesis,
    val experiments: List<FirebaseExperiment>
) 