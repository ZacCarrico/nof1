package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Primary Project model for Firebase Firestore storage.
 * This replaces the Room Project entity.
 */
data class FirebaseProject(
    @DocumentId
    val id: String = "",
    
    val name: String = "",
    val goal: String = "",
    val archived: Boolean = false,
    val userId: String = "", // For multi-user support
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", false, "", null, null)
    
    /**
     * Get createdAt as LocalDateTime for UI display
     */
    fun getCreatedAtAsLocalDateTime(): LocalDateTime {
        return createdAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        } ?: LocalDateTime.now()
    }
    
    /**
     * Get updatedAt as LocalDateTime for UI display
     */
    fun getUpdatedAtAsLocalDateTime(): LocalDateTime {
        return updatedAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        } ?: LocalDateTime.now()
    }
    
    /**
     * Create a copy with updated timestamp
     */
    fun copyWithUpdatedTimestamp(): FirebaseProject {
        return this.copy(updatedAt = null) // Firebase will set this with @ServerTimestamp
    }
} 