package com.nof1.data.repository

import com.nof1.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Firebase-only repository for accessing ReminderSettings data.
 * This replaces the hybrid repository pattern.
 */
class ReminderRepository : BaseFirebaseRepository() {
    
    private val remindersCollection = firestore.collection("reminders")
    
    /**
     * Insert a new reminder setting
     */
    suspend fun insertReminderSettings(reminderSettings: ReminderSettings): String? {
        val userId = requireUserId()
        val firebaseReminderSettings = reminderSettings.copy(userId = userId)
        return addDocument(remindersCollection, firebaseReminderSettings)
    }
    
    /**
     * Insert multiple reminder settings
     */
    suspend fun insertAllReminderSettings(reminderSettingsList: List<ReminderSettings>): List<String> {
        val userId = requireUserId()
        val documentIds = mutableListOf<String>()
        
        reminderSettingsList.forEach { reminderSettings ->
            val firebaseReminderSettings = reminderSettings.copy(userId = userId)
            val documentId = addDocument(remindersCollection, firebaseReminderSettings)
            documentId?.let { documentIds.add(it) }
        }
        
        return documentIds
    }
    
    /**
     * Update an existing reminder setting
     */
    suspend fun updateReminderSettings(reminderSettings: ReminderSettings): Boolean {
        val userId = requireUserId()
        val updatedReminderSettings = reminderSettings.copy(userId = userId)
        return updateDocument(remindersCollection, reminderSettings.id, updatedReminderSettings)
    }
    
    /**
     * Delete a reminder setting
     */
    suspend fun deleteReminderSettings(reminderSettings: ReminderSettings): Boolean {
        return deleteDocument(remindersCollection, reminderSettings.id)
    }
    
    /**
     * Get reminder settings by ID
     */
    suspend fun getReminderById(reminderId: String): ReminderSettings? {
        return getDocumentById<ReminderSettings>(remindersCollection, reminderId)
    }
    
    /**
     * Get reminder settings for a specific entity (project or hypothesis)
     */
    fun getReminderSettingsForEntity(entityType: String, entityId: String): Flow<List<ReminderSettings>> {
        val userId = requireUserId()
        return getCollectionAsFlow<ReminderSettings>(remindersCollection) { collection ->
            collection
                .whereEqualTo("entityType", entityType)
                .whereEqualTo("entityId", entityId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Get reminder settings for entity synchronously
     */
    suspend fun getReminderSettingsForEntitySync(entityType: String, entityId: String): List<ReminderSettings> {
        val userId = requireUserId()
        return try {
            remindersCollection
                .whereEqualTo("entityType", entityType)
                .whereEqualTo("entityId", entityId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ReminderSettings::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get all active reminders (enabled reminders)
     */
    suspend fun getAllActiveReminders(): List<ReminderSettings> {
        val userId = requireUserId()
        return try {
            remindersCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isEnabled", true)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject(ReminderSettings::class.java) }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Get all reminders for a project
     */
    fun getRemindersForProject(projectId: String): Flow<List<ReminderSettings>> {
        val userId = requireUserId()
        return getCollectionAsFlow<ReminderSettings>(remindersCollection) { collection ->
            collection
                .whereEqualTo("projectId", projectId)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
        }
    }
    
    /**
     * Delete all reminders for a specific entity
     */
    suspend fun deleteAllRemindersForEntity(entityType: String, entityId: String): Boolean {
        return try {
            val userId = requireUserId()
            val querySnapshot = remindersCollection
                .whereEqualTo("entityType", entityType)
                .whereEqualTo("entityId", entityId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            // Delete each document
            querySnapshot.documents.forEach { document ->
                document.reference.delete().await()
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Delete reminder by ID
     */
    suspend fun deleteReminderById(reminderId: String): Boolean {
        return deleteDocument(remindersCollection, reminderId)
    }
    
    /**
     * Toggle reminder enabled/disabled
     */
    suspend fun toggleReminderEnabled(reminderId: String, isEnabled: Boolean): Boolean {
        val reminder = getReminderById(reminderId)
        return if (reminder != null) {
            val updatedReminder = reminder.copy(isEnabled = isEnabled)
            updateReminderSettings(updatedReminder)
        } else {
            false
        }
    }
}