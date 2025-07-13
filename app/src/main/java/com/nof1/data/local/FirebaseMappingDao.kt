package com.nof1.data.local

import androidx.room.*
import com.nof1.data.model.FirebaseMapping

/**
 * Data Access Object for Firebase ID mappings.
 */
@Dao
interface FirebaseMappingDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMapping(mapping: FirebaseMapping): Long
    
    @Update
    suspend fun updateMapping(mapping: FirebaseMapping)
    
    @Delete
    suspend fun deleteMapping(mapping: FirebaseMapping)
    
    @Query("SELECT * FROM firebase_mappings WHERE entityType = :entityType AND localId = :localId AND userId = :userId")
    suspend fun getFirebaseIdByLocalId(entityType: String, localId: Long, userId: String): FirebaseMapping?
    
    @Query("SELECT * FROM firebase_mappings WHERE entityType = :entityType AND firebaseId = :firebaseId AND userId = :userId")
    suspend fun getLocalIdByFirebaseId(entityType: String, firebaseId: String, userId: String): FirebaseMapping?
    
    @Query("SELECT * FROM firebase_mappings WHERE entityType = :entityType AND userId = :userId")
    suspend fun getAllMappingsForEntityType(entityType: String, userId: String): List<FirebaseMapping>
    
    @Query("DELETE FROM firebase_mappings WHERE entityType = :entityType AND localId = :localId AND userId = :userId")
    suspend fun deleteMappingByLocalId(entityType: String, localId: Long, userId: String)
    
    @Query("DELETE FROM firebase_mappings WHERE entityType = :entityType AND firebaseId = :firebaseId AND userId = :userId")
    suspend fun deleteMappingByFirebaseId(entityType: String, firebaseId: String, userId: String)
    
    @Query("DELETE FROM firebase_mappings WHERE userId = :userId")
    suspend fun deleteAllMappingsForUser(userId: String)
} 