package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firebase-compatible version of Hypothesis for Firestore storage.
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
     * Convert to Room Hypothesis for local storage/offline support
     */
    fun toHypothesis(roomProjectId: Long): Hypothesis {
        return Hypothesis(
            id = 0, // Room will auto-generate
            projectId = roomProjectId,
            name = name,
            description = description,
            isArchived = isArchived,
            createdAt = createdAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            } ?: java.time.LocalDateTime.now(),
            updatedAt = updatedAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            } ?: java.time.LocalDateTime.now()
        )
    }
}

/**
 * Extension function to convert Room Hypothesis to Firebase Hypothesis
 */
fun Hypothesis.toFirebaseHypothesis(userId: String, firebaseProjectId: String, firebaseId: String = ""): FirebaseHypothesis {
    return FirebaseHypothesis(
        id = firebaseId,
        projectId = firebaseProjectId,
        name = name,
        description = description,
        isArchived = isArchived,
        userId = userId,
        createdAt = Timestamp(java.util.Date.from(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant())),
        updatedAt = Timestamp(java.util.Date.from(updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()))
    )
} 