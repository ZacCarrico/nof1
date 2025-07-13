package com.nof1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Entity to track mappings between local Room IDs and Firebase document IDs.
 * This is essential for the hybrid repository pattern to work properly.
 */
@Entity(tableName = "firebase_mappings")
data class FirebaseMapping(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val entityType: String, // "project", "hypothesis", "experiment", "log_entry"
    val localId: Long,
    val firebaseId: String,
    val userId: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) 