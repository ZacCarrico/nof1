package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

/**
 * Firebase-only repository for accessing Experiment data.
 * This replaces the hybrid repository pattern.
 */
class ExperimentRepository : BaseFirebaseRepository() {
    
    private val experimentsCollection = firestore.collection("experiments")
    
    /**
     * Insert a new experiment
     */
    suspend fun insertExperiment(experiment: Experiment): String? {
        val userId = requireUserId()
        val firebaseExperiment = experiment.copy(userId = userId)
        return addDocument(experimentsCollection, firebaseExperiment)
    }
    
    /**
     * Update an existing experiment
     */
    suspend fun updateExperiment(experiment: Experiment): Boolean {
        val userId = requireUserId()
        val updatedExperiment = experiment.copy(userId = userId).copyWithUpdatedTimestamp()
        return updateDocument(experimentsCollection, experiment.id, updatedExperiment)
    }
    
    /**
     * Delete an experiment
     */
    suspend fun deleteExperiment(experiment: Experiment): Boolean {
        return deleteDocument(experimentsCollection, experiment.id)
    }
    
    /**
     * Archive an experiment
     */
    suspend fun archiveExperiment(experiment: Experiment): Boolean {
        val archivedExperiment = experiment.copy(isArchived = true).copyWithUpdatedTimestamp()
        return updateDocument(experimentsCollection, experiment.id, archivedExperiment)
    }
    
    /**
     * Update last logged at timestamp
     */
    suspend fun updateLastLoggedAt(experimentId: String): Boolean {
        val experiment = getExperimentById(experimentId)
        return if (experiment != null) {
            val updatedExperiment = experiment.updateLastLoggedAt()
            updateDocument(experimentsCollection, experimentId, updatedExperiment)
        } else {
            false
        }
    }
    
    /**
     * Update last notification sent timestamp
     */
    suspend fun updateLastNotificationSent(experimentId: String, timestamp: LocalDateTime): Boolean {
        val experiment = getExperimentById(experimentId)
        return if (experiment != null) {
            val updatedExperiment = experiment.updateLastNotificationSent(timestamp)
            updateDocument(experimentsCollection, experimentId, updatedExperiment)
        } else {
            false
        }
    }
    
    /**
     * Get experiment by ID
     */
    suspend fun getExperimentById(experimentId: String): Experiment? {
        return getDocumentById<Experiment>(experimentsCollection, experimentId)
    }
    
    /**
     * Get all active experiments for a hypothesis
     */
    fun getActiveExperimentsForHypothesis(hypothesisId: String): Flow<List<Experiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Experiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isArchived", false)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get all experiments for a hypothesis
     */
    fun getAllExperimentsForHypothesis(hypothesisId: String): Flow<List<Experiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Experiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .orderBy("updatedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get experiments with notifications enabled
     */
    fun getExperimentsWithNotificationsEnabled(): Flow<List<Experiment>> {
        val userId = requireUserId()
        return getCollectionAsFlow<Experiment>(experimentsCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .whereEqualTo("notificationsEnabled", true)
                .whereEqualTo("isArchived", false)
        }
    }
    
    /**
     * Get experiment with its log entries
     */
    fun getExperimentWithLogs(experimentId: String): Flow<ExperimentWithLogs?> = flow {
        try {
            val experiment = getExperimentById(experimentId)
            if (experiment != null) {
                val logEntries = getLogEntriesForExperiment(experimentId)
                emit(ExperimentWithLogs(experiment, logEntries))
            } else {
                emit(null)
            }
        } catch (e: Exception) {
            emit(null)
        }
    }
    
    /**
     * Get all active experiments with logs for a hypothesis
     */
    fun getActiveExperimentsWithLogsForHypothesis(hypothesisId: String): Flow<List<ExperimentWithLogs>> = flow {
        try {
            getActiveExperimentsForHypothesis(hypothesisId).collect { experiments ->
                val experimentsWithLogs = experiments.map { experiment ->
                    val logEntries = getLogEntriesForExperiment(experiment.id)
                    ExperimentWithLogs(experiment, logEntries)
                }
                emit(experimentsWithLogs)
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    // Helper methods
    private suspend fun getLogEntriesForExperiment(experimentId: String): List<LogEntry> {
        val userId = requireUserId()
        return try {
            firestore.collection("log_entries")
                .whereEqualTo("experimentId", experimentId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(LogEntry::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
} 