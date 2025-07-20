package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-only repository for accessing Hypothesis data.
 * This replaces the hybrid repository pattern.
 */
class HypothesisRepository : BaseFirebaseRepository(), HypothesisRepositoryInterface {
    
    private val hypothesesCollection = firestore.collection("hypotheses")
    
    /**
     * Insert a new hypothesis
     */
    override suspend fun insertHypothesis(hypothesis: Hypothesis): String? {
        val userId = requireUserId()
        val firebaseHypothesis = hypothesis.copy(userId = userId)
        return addDocument(hypothesesCollection, firebaseHypothesis)
    }
    
    /**
     * Update an existing hypothesis
     */
    override suspend fun updateHypothesis(hypothesis: Hypothesis): Boolean {
        val userId = requireUserId()
        val updatedHypothesis = hypothesis.copy(userId = userId).copyWithUpdatedTimestamp()
        return updateDocument(hypothesesCollection, hypothesis.id, updatedHypothesis)
    }
    
    /**
     * Delete a hypothesis
     */
    override suspend fun deleteHypothesis(hypothesis: Hypothesis): Boolean {
        return deleteDocument(hypothesesCollection, hypothesis.id)
    }
    
    /**
     * Archive a hypothesis
     */
    override suspend fun archiveHypothesis(hypothesis: Hypothesis): Boolean {
        val archivedHypothesis = hypothesis.copy(archived = true).copyWithUpdatedTimestamp()
        return updateDocument(hypothesesCollection, hypothesis.id, archivedHypothesis)
    }
    
    /**
     * Get hypothesis by ID
     */
    suspend fun getHypothesisById(hypothesisId: String): Hypothesis? {
        return getDocumentById<Hypothesis>(hypothesesCollection, hypothesisId)
    }
    
    /**
     * Get all active hypotheses for a project
     */
    fun getActiveHypothesesForProject(projectId: String): Flow<List<Hypothesis>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Hypothesis>(hypothesesCollection) { collection ->
            collection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("archived", false)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get all hypotheses for a project
     */
    fun getAllHypothesesForProject(projectId: String): Flow<List<Hypothesis>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Hypothesis>(hypothesesCollection) { collection ->
            collection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get hypothesis with its experiments
     */
    fun getHypothesisWithExperiments(hypothesisId: String): Flow<HypothesisWithExperiments?> = flow {
        try {
            val hypothesis = getHypothesisById(hypothesisId)
            if (hypothesis != null) {
                val experiments = getExperimentsForHypothesis(hypothesisId)
                emit(HypothesisWithExperiments(hypothesis, experiments))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    /**
     * Get hypothesis with its notes
     */
    fun getHypothesisWithNotes(hypothesisId: String): Flow<HypothesisWithNotes?> = flow {
        try {
            val hypothesis = getHypothesisById(hypothesisId)
            if (hypothesis != null) {
                val notes = getNotesForHypothesis(hypothesisId)
                emit(HypothesisWithNotes(hypothesis, notes))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    /**
     * Get all active hypotheses with experiments for a project
     */
    fun getActiveHypothesesWithExperimentsForProject(projectId: String): Flow<List<HypothesisWithExperiments>> = flow {
        try {
            getActiveHypothesesForProject(projectId).collect { hypotheses ->
                val hypothesesWithExperiments = hypotheses.map { hypothesis ->
                    val experiments = getExperimentsForHypothesis(hypothesis.id)
                    HypothesisWithExperiments(hypothesis, experiments)
                }
                emit(hypothesesWithExperiments)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    /**
     * Get all hypotheses with experiments for a project
     */
    fun getAllHypothesesWithExperimentsForProject(projectId: String): Flow<List<HypothesisWithExperiments>> = flow {
        try {
            getAllHypothesesForProject(projectId).collect { hypotheses ->
                val hypothesesWithExperiments = hypotheses.map { hypothesis ->
                    val experiments = getExperimentsForHypothesis(hypothesis.id)
                    HypothesisWithExperiments(hypothesis, experiments)
                }
                emit(hypothesesWithExperiments)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Helper methods
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
    
    private suspend fun getNotesForHypothesis(hypothesisId: String): List<Note> {
        val userId = requireUserId()
        return try {
            firestore.collection("notes")
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(Note::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
} 