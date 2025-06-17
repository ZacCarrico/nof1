package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
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
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
            collection
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getActiveHypothesesByProject(firebaseProjectId: String): Flow<List<FirebaseHypothesis>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
            collection
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getArchivedHypothesesByProject(firebaseProjectId: String): Flow<List<FirebaseHypothesis>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseHypothesis>(hypothesesCollection) { collection ->
            collection
                .whereEqualTo("projectId", firebaseProjectId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
} 