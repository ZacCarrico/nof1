package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firebase-compatible version of Project for Firestore storage.
 * Uses Firestore-specific annotations and types.
 */
data class FirebaseProject(
    @DocumentId
    val id: String = "",
    
    val name: String = "",
    val goal: String = "",
    val isArchived: Boolean = false,
    val userId: String = "", // For multi-user support
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", false, "", null, null)
    
    /**
     * Convert to Room Project for local storage/offline support
     */
    fun toProject(): Project {
        return Project(
            id = 0, // Room will auto-generate
            name = name,
            goal = goal,
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
 * Extension function to convert Room Project to Firebase Project
 */
fun Project.toFirebaseProject(userId: String, firebaseId: String = ""): FirebaseProject {
    return FirebaseProject(
        id = firebaseId,
        name = name,
        goal = goal,
        isArchived = isArchived,
        userId = userId,
        createdAt = Timestamp(java.util.Date.from(createdAt.atZone(java.time.ZoneId.systemDefault()).toInstant())),
        updatedAt = Timestamp(java.util.Date.from(updatedAt.atZone(java.time.ZoneId.systemDefault()).toInstant()))
    )
} 