package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Primary LogEntry model for Firebase Firestore storage.
 * This replaces the Room LogEntry entity.
 */
data class FirebaseLogEntry(
    @DocumentId
    val id: String = "",
    
    val experimentId: String = "",
    val hypothesisId: String = "", // For easier querying
    val projectId: String = "", // For easier querying
    val response: String = "",
    val isFromNotification: Boolean = false,
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", "", "", false, "", null)
    
    /**
     * Get createdAt as LocalDateTime for UI display
     */
    @Exclude
    fun getCreatedAtAsLocalDateTime(): LocalDateTime {
        return createdAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        } ?: LocalDateTime.now()
    }
} 