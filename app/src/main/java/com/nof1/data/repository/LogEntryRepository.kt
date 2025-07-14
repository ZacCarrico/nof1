package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.time.LocalDateTime

/**
 * Firebase-only repository for accessing LogEntry data.
 * This replaces the hybrid repository pattern.
 */
class LogEntryRepository : BaseFirebaseRepository() {
    
    private val logEntriesCollection = firestore.collection("log_entries")
    
    /**
     * Insert a new log entry
     */
    suspend fun insertLogEntry(logEntry: LogEntry): String? {
        val userId = requireUserId()
        val firebaseLogEntry = logEntry.copy(userId = userId)
        return addDocument(logEntriesCollection, firebaseLogEntry)
    }
    
    /**
     * Update an existing log entry
     */
    suspend fun updateLogEntry(logEntry: LogEntry): Boolean {
        val userId = requireUserId()
        val updatedLogEntry = logEntry.copy(userId = userId)
        return updateDocument(logEntriesCollection, logEntry.id, updatedLogEntry)
    }
    
    /**
     * Delete a log entry
     */
    suspend fun deleteLogEntry(logEntry: LogEntry): Boolean {
        return deleteDocument(logEntriesCollection, logEntry.id)
    }
    
    /**
     * Get log entry by ID
     */
    suspend fun getLogEntryById(logEntryId: String): LogEntry? {
        return getDocumentById<LogEntry>(logEntriesCollection, logEntryId)
    }
    
    /**
     * Get all log entries for an experiment
     */
    fun getLogEntriesForExperiment(experimentId: String): Flow<List<LogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<LogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("experimentId", experimentId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get latest log entry for an experiment after a specific timestamp
     */
    suspend fun getLatestLogEntryAfterTimestamp(experimentId: String, timestamp: LocalDateTime): LogEntry? {
        val userId = requireUserId()
        return try {
            val timestampFirebase = com.google.firebase.Timestamp(
                java.util.Date.from(timestamp.atZone(java.time.ZoneId.systemDefault()).toInstant())
            )
            
            val querySnapshot = logEntriesCollection
                .whereEqualTo("experimentId", experimentId)
                .whereEqualTo("userId", userId)
                .whereGreaterThan("createdAt", timestampFirebase)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                
            querySnapshot.documents.firstOrNull()?.toObject(LogEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get latest log entry for an experiment
     */
    suspend fun getLatestLogEntryForExperiment(experimentId: String): LogEntry? {
        val userId = requireUserId()
        return try {
            val querySnapshot = logEntriesCollection
                .whereEqualTo("experimentId", experimentId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                
            querySnapshot.documents.firstOrNull()?.toObject(LogEntry::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Get all log entries for a hypothesis
     */
    fun getLogEntriesForHypothesis(hypothesisId: String): Flow<List<LogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<LogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("hypothesisId", hypothesisId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get all log entries for a project
     */
    fun getLogEntriesForProject(projectId: String): Flow<List<LogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<LogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
} 