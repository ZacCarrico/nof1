package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of Experiment repository.
 */
class FirebaseExperimentRepository : BaseFirebaseRepository() {
    
    private val experimentsCollection = firestore.collection("experiments")
    
    suspend fun insertExperiment(experiment: Experiment, firebaseHypothesisId: String, firebaseProjectId: String): String? {
        val userId = requireUserId()
        val firebaseExperiment = experiment.toFirebaseExperiment(userId, firebaseHypothesisId, firebaseProjectId)
        return addDocument(experimentsCollection, firebaseExperiment)
    }
    
    suspend fun updateExperiment(firebaseExperimentId: String, experiment: Experiment, firebaseHypothesisId: String, firebaseProjectId: String): Boolean {
        val userId = requireUserId()
        val firebaseExperiment = experiment.toFirebaseExperiment(userId, firebaseHypothesisId, firebaseProjectId, firebaseExperimentId)
        return updateDocument(experimentsCollection, firebaseExperimentId, firebaseExperiment)
    }
    
    suspend fun deleteExperiment(firebaseExperimentId: String): Boolean {
        return deleteDocument(experimentsCollection, firebaseExperimentId)
    }
    
    suspend fun archiveExperiment(firebaseExperimentId: String, experiment: Experiment, firebaseHypothesisId: String, firebaseProjectId: String): Boolean {
        val userId = requireUserId()
        val archivedExperiment = experiment.copy(isArchived = true)
        val firebaseExperiment = archivedExperiment.toFirebaseExperiment(userId, firebaseHypothesisId, firebaseProjectId, firebaseExperimentId)
        return updateDocument(experimentsCollection, firebaseExperimentId, firebaseExperiment)
    }
    
    suspend fun getExperimentById(firebaseExperimentId: String): FirebaseExperiment? {
        return getDocumentById<FirebaseExperiment>(experimentsCollection, firebaseExperimentId)
    }
    
    fun getExperimentsByHypothesis(firebaseHypothesisId: String): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", firebaseHypothesisId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getActiveExperimentsByHypothesis(firebaseHypothesisId: String): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", firebaseHypothesisId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getArchivedExperimentsByHypothesis(firebaseHypothesisId: String): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", firebaseHypothesisId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getExperimentsWithNotifications(): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("notificationsEnabled", true)
                .whereEqualTo("isArchived", false)
        }
    }
    
    fun getExperimentsByProject(firebaseProjectId: String): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getActiveExperimentsByProject(firebaseProjectId: String): Flow<List<FirebaseExperiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseExperiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
} 