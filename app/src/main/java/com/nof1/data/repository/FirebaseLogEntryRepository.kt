package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase implementation of LogEntry repository.
 */
class FirebaseLogEntryRepository : BaseFirebaseRepository() {
    
    private val logEntriesCollection = firestore.collection("log_entries")
    
    suspend fun insertLogEntry(logEntry: LogEntry, firebaseExperimentId: String): String? {
        val userId = requireUserId()
        val firebaseLogEntry = logEntry.toFirebaseLogEntry(userId, firebaseExperimentId)
        return addDocument(logEntriesCollection, firebaseLogEntry)
    }
    
    suspend fun updateLogEntry(firebaseLogEntryId: String, logEntry: LogEntry, firebaseExperimentId: String): Boolean {
        val userId = requireUserId()
        val firebaseLogEntry = logEntry.toFirebaseLogEntry(userId, firebaseExperimentId, firebaseLogEntryId)
        return updateDocument(logEntriesCollection, firebaseLogEntryId, firebaseLogEntry)
    }
    
    suspend fun deleteLogEntry(firebaseLogEntryId: String): Boolean {
        return deleteDocument(logEntriesCollection, firebaseLogEntryId)
    }
    
    suspend fun getLogEntryById(firebaseLogEntryId: String): FirebaseLogEntry? {
        return getDocumentById<FirebaseLogEntry>(logEntriesCollection, firebaseLogEntryId)
    }
    
    fun getLogEntriesByExperiment(firebaseExperimentId: String): Flow<List<FirebaseLogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseLogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("experimentId", firebaseExperimentId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getRecentLogEntriesByExperiment(firebaseExperimentId: String, limit: Int = 50): Flow<List<FirebaseLogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseLogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("experimentId", firebaseExperimentId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(limit.toLong())
        }
    }
    
    fun getLogEntriesFromNotifications(firebaseExperimentId: String): Flow<List<FirebaseLogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseLogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("experimentId", firebaseExperimentId)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isFromNotification", true)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    fun getAllLogEntriesForUser(): Flow<List<FirebaseLogEntry>> {
        val userId = requireUserId()
        return getCollectionAsFlow<FirebaseLogEntry>(logEntriesCollection) { collection ->
            collection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
} 