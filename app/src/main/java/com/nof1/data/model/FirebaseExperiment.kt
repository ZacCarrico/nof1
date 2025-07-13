package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

/**
 * Firebase-compatible version of Experiment for Firestore storage.
 */
data class FirebaseExperiment(
    @DocumentId
    val id: String = "",
    
    val hypothesisId: String = "",
    val projectId: String = "",
    val name: String = "",
    val description: String = "",
    val question: String = "",
    
    // Notification settings
    val notificationsEnabled: Boolean = true,
    val notificationFrequency: String = "DAILY", // Store enum as string
    val notificationTimeHour: Int = 9,
    val notificationTimeMinute: Int = 0,
    val customFrequencyDays: Int? = null,
    
    val isArchived: Boolean = false,
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null,
    
    val lastNotificationSent: Timestamp? = null,
    val lastLoggedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", "", "", "", true, "DAILY", 9, 0, null, false, "", null, null, null, null)
    
    /**
     * Convert to Room Experiment for local storage/offline support
     */
    fun toExperiment(roomHypothesisId: Long): Experiment {
        return Experiment(
            id = 0, // Room will auto-generate
            hypothesisId = roomHypothesisId,
            name = name,
            description = description,
            question = question,
            notificationsEnabled = notificationsEnabled,
            notificationFrequency = NotificationFrequency.valueOf(notificationFrequency),
            notificationTime = java.time.LocalTime.of(notificationTimeHour, notificationTimeMinute),
            customFrequencyDays = customFrequencyDays,
            isArchived = isArchived,
            createdAt = createdAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            } ?: java.time.LocalDateTime.now(),
            updatedAt = updatedAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            } ?: java.time.LocalDateTime.now(),
            lastNotificationSent = lastNotificationSent?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            },
            lastLoggedAt = lastLoggedAt?.toDate()?.let { 
                java.time.LocalDateTime.ofInstant(it.toInstant(), java.time.ZoneId.systemDefault()) 
            }
        )
    }
}

/**
 * Extension function to convert Room Experiment to Firebase Experiment
 */
fun Experiment.toFirebaseExperiment(userId: String, firebaseHypothesisId: String, firebaseProjectId: String, firebaseId: String = ""): FirebaseExperiment {
    return FirebaseExperiment(
        id = firebaseId,
        hypothesisId = firebaseHypothesisId,
        projectId = firebaseProjectId,
        name = name,
        description = description,
        question = question,
        notificationsEnabled = notificationsEnabled,
        notificationFrequency = notificationFrequency.name,
        notificationTimeHour = notificationTime.hour,
        notificationTimeMinute = notificationTime.minute,
        customFrequencyDays = customFrequencyDays,
        isArchived = isArchived,
        userId = userId,
        // Let Firebase set these automatically with @ServerTimestamp
        createdAt = null,
        updatedAt = null,
        lastNotificationSent = lastNotificationSent?.let { 
            Timestamp(java.util.Date.from(it.atZone(java.time.ZoneId.systemDefault()).toInstant())) 
        },
        lastLoggedAt = lastLoggedAt?.let { 
            Timestamp(java.util.Date.from(it.atZone(java.time.ZoneId.systemDefault()).toInstant())) 
        }
    )
} 