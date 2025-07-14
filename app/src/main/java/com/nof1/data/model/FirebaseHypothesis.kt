package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Primary Hypothesis model for Firebase Firestore storage.
 * This replaces the Room Hypothesis entity.
 */
data class FirebaseHypothesis(
    @DocumentId
    val id: String = "",
    
    val projectId: String = "",
    val name: String = "",
    val description: String = "",
    val isArchived: Boolean = false,
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", "", false, "", null, null)
    
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
    fun copyWithUpdatedTimestamp(): FirebaseHypothesis {
        return this.copy(updatedAt = null) // Firebase will set this with @ServerTimestamp
    }
} 