package com.nof1.data.repository

import com.nof1.data.local.FirebaseMappingDao
import com.nof1.data.model.FirebaseMapping
import com.nof1.utils.AuthManager

/**
 * Repository for managing Firebase ID mappings.
 * This is essential for the hybrid repository pattern to work properly.
 */
class FirebaseMappingRepository(
    private val mappingDao: FirebaseMappingDao,
    private val authManager: AuthManager
) {
    
    /**
     * Store mapping between local ID and Firebase ID
     */
    suspend fun storeMapping(
        entityType: String,
        localId: Long,
        firebaseId: String
    ) {
        val userId = authManager.currentUserId ?: return
        
        val mapping = FirebaseMapping(
            entityType = entityType,
            localId = localId,
            firebaseId = firebaseId,
            userId = userId
        )
        
        mappingDao.insertMapping(mapping)
    }
    
    /**
     * Get Firebase ID for a local ID
     */
    suspend fun getFirebaseId(
        entityType: String,
        localId: Long
    ): String? {
        val userId = authManager.currentUserId ?: return null
        
        return mappingDao.getFirebaseIdByLocalId(entityType, localId, userId)?.firebaseId
    }
    
    /**
     * Get local ID for a Firebase ID
     */
    suspend fun getLocalId(
        entityType: String,
        firebaseId: String
    ): Long? {
        val userId = authManager.currentUserId ?: return null
        
        return mappingDao.getLocalIdByFirebaseId(entityType, firebaseId, userId)?.localId
    }
    
    /**
     * Delete mapping by local ID
     */
    suspend fun deleteMappingByLocalId(
        entityType: String,
        localId: Long
    ) {
        val userId = authManager.currentUserId ?: return
        
        mappingDao.deleteMappingByLocalId(entityType, localId, userId)
    }
    
    /**
     * Delete mapping by Firebase ID
     */
    suspend fun deleteMappingByFirebaseId(
        entityType: String,
        firebaseId: String
    ) {
        val userId = authManager.currentUserId ?: return
        
        mappingDao.deleteMappingByFirebaseId(entityType, firebaseId, userId)
    }
    
    /**
     * Get all mappings for a specific entity type
     */
    suspend fun getAllMappingsForEntityType(
        entityType: String
    ): List<FirebaseMapping> {
        val userId = authManager.currentUserId ?: return emptyList()
        
        return mappingDao.getAllMappingsForEntityType(entityType, userId)
    }
    
    /**
     * Clear all mappings for current user (useful for logout)
     */
    suspend fun clearAllMappingsForCurrentUser() {
        val userId = authManager.currentUserId ?: return
        
        mappingDao.deleteAllMappingsForUser(userId)
    }
    
    companion object {
        const val ENTITY_TYPE_PROJECT = "project"
        const val ENTITY_TYPE_HYPOTHESIS = "hypothesis"
        const val ENTITY_TYPE_EXPERIMENT = "experiment"
        const val ENTITY_TYPE_LOG_ENTRY = "log_entry"
    }
} 