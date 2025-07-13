package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of Hypothesis repository.
 */
class FirebaseHypothesisRepository : BaseFirebaseRepository() {
    
    private val hypothesesCollection = firestore.collection("hypotheses")
    
    suspend fun insertHypothesis(hypothesis: Hypothesis, firebaseProjectId: String): String? {
        val userId = requireUserId()
        val firebaseHypothesis = hypothesis.toFirebaseHypothesis(userId, firebaseProjectId)
        return addDocument(hypothesesCollection, firebaseHypothesis)
    }
    
    suspend fun updateHypothesis(firebaseHypothesisId: String, hypothesis: Hypothesis, firebaseProjectId: String): Boolean {
        val userId = requireUserId()
        val firebaseHypothesis = hypothesis.toFirebaseHypothesis(userId, firebaseProjectId, firebaseHypothesisId)
        return updateDocument(hypothesesCollection, firebaseHypothesisId, firebaseHypothesis)
    }
    
    suspend fun deleteHypothesis(firebaseHypothesisId: String): Boolean {
        return deleteDocument(hypothesesCollection, firebaseHypothesisId)
    }
    
    suspend fun archiveHypothesis(firebaseHypothesisId: String, hypothesis: Hypothesis, firebaseProjectId: String): Boolean {
        val userId = requireUserId()
        val archivedHypothesis = hypothesis.copy(isArchived = true)
        val firebaseHypothesis = archivedHypothesis.toFirebaseHypothesis(userId, firebaseProjectId, firebaseHypothesisId)
        return updateDocument(hypothesesCollection, firebaseHypothesisId, firebaseHypothesis)
    }
    
    suspend fun getHypothesisById(firebaseHypothesisId: String): FirebaseHypothesis? {
        return getDocumentById<FirebaseHypothesis>(hypothesesCollection, firebaseHypothesisId)
    }
    
    fun getHypothesesByProject(firebaseProjectId: String): Flow<List<FirebaseHypothesis>> {
        return flow {
            try {
                val userId = requireUserId()
                android.util.Log.d("FirebaseHypothesisRepository", "Getting hypotheses for project: $firebaseProjectId, user: $userId")
                getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
                    collection
                        .whereEqualTo("projectId", firebaseProjectId)
                        .whereEqualTo("userId", userId)
                        // Temporarily removing orderBy to test basic data loading
                        // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }.collect { hypotheses ->
                    android.util.Log.d("FirebaseHypothesisRepository", "Firebase returned ${hypotheses.size} hypotheses for project $firebaseProjectId")
                    emit(hypotheses)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseHypothesisRepository", "Error getting hypotheses for project $firebaseProjectId: ${e.message}", e)
                emit(emptyList<FirebaseHypothesis>())
            }
        }
    }
    
    fun getActiveHypothesesByProject(firebaseProjectId: String): Flow<List<FirebaseHypothesis>> {
        return flow {
            try {
                val userId = requireUserId()
                getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
                    collection
                        .whereEqualTo("projectId", firebaseProjectId)
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("isArchived", false)
                        // Temporarily removing orderBy to test basic data loading
                        // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }.collect { hypotheses ->
                    emit(hypotheses)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                android.util.Log.d("FirebaseHypothesisRepository", "Active hypotheses collection cancelled")
                throw e
            } catch (e: Exception) {
                android.util.Log.e("FirebaseHypothesisRepository", "Error getting active hypotheses: ${e.message}", e)
                emit(emptyList<FirebaseHypothesis>())
            }
        }
    }
    
    fun getArchivedHypothesesByProject(firebaseProjectId: String): Flow<List<FirebaseHypothesis>> {
        return flow {
            try {
                val userId = requireUserId()
                getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
                    collection
                        .whereEqualTo("projectId", firebaseProjectId)
                        .whereEqualTo("userId", userId)
                        .whereEqualTo("isArchived", true)
                        // Temporarily removing orderBy to test basic data loading
                        // .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                }.collect { hypotheses ->
                    emit(hypotheses)
                }
            } catch (e: Exception) {
                android.util.Log.e("FirebaseHypothesisRepository", "Error getting archived hypotheses: ${e.message}", e)
                emit(emptyList<FirebaseHypothesis>())
            }
        }
    }
} 