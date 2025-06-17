package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firebase-compatible version of LogEntry for Firestore storage.
 */
data class FirebaseLogEntry(
    @DocumentId
    val id: String = "",
    
    val experimentId: String = "",
    val response: String = "",
    val isFromNotification: Boolean = false,
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", false, "", null)
    
    /**
     * Convert to Room LogEntry for local storage/offline support
     */
    fun toLogEntry(roomExperimentId: Long): LogEntry {
        return LogEntry(
            id = 0, // Room will auto-generate
            experimentId = roomExperimentId,
            response = response,
            isFromNotification = isFromNotification,
            createdAt = createdAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            } ?: java.time.LocalDateTime.now()
        )
    }
}

/**
 * Extension function to convert Room LogEntry to Firebase LogEntry
 */
fun LogEntry.toFirebaseLogEntry(userId: String, firebaseExperimentId: String, firebaseId: String = ""): FirebaseLogEntry {
    return FirebaseLogEntry(
        id = firebaseId,
        experimentId = firebaseExperimentId,
        response = response,
        isFromNotification = isFromNotification,
        userId = userId,
        createdAt = Timestamp(java.util.Date.from(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant()))
    )
} 