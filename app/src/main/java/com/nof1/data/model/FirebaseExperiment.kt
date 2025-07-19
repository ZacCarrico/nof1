package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Primary Experiment model for Firebase Firestore storage.
 * This replaces the Room Experiment entity.
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
     * Get createdAt as LocalDateTime for UI display
     */
    @Exclude
    fun getCreatedAtAsLocalDateTime(): LocalDateTime {
        return createdAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        } ?: LocalDateTime.now()
    }
    
    /**
     * Get updatedAt as LocalDateTime for UI display
     */
    @Exclude
    fun getUpdatedAtAsLocalDateTime(): LocalDateTime {
        return updatedAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        } ?: LocalDateTime.now()
    }
    
    /**
     * Get notification time as LocalTime for UI display
     */
    @Exclude
    fun getNotificationTime(): LocalTime {
        return LocalTime.of(notificationTimeHour, notificationTimeMinute)
    }
    
    /**
     * Get notification frequency as enum
     */
    @Exclude
    fun getNotificationFrequencyEnum(): NotificationFrequency {
        return try {
            NotificationFrequency.valueOf(notificationFrequency)
        } catch (e: IllegalArgumentException) {
            NotificationFrequency.DAILY
        }
    }
    
    /**
     * Get lastNotificationSent as LocalDateTime for UI display
     */
    @Exclude
    fun getLastNotificationSentAsLocalDateTime(): LocalDateTime? {
        return lastNotificationSent?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        }
    }
    
    /**
     * Get lastLoggedAt as LocalDateTime for UI display
     */
    @Exclude
    fun getLastLoggedAtAsLocalDateTime(): LocalDateTime? {
        return lastLoggedAt?.toDate()?.let { 
            LocalDateTime.ofInstant(it.toInstant(), ZoneId.systemDefault()) 
        }
    }
    
    /**
     * Create a copy with updated timestamp
     */
    @Exclude
    fun copyWithUpdatedTimestamp(): FirebaseExperiment {
        return this.copy(updatedAt = null) // Firebase will set this with @ServerTimestamp
    }
    
    /**
     * Update last notification sent timestamp
     */
    @Exclude
    fun updateLastNotificationSent(timestamp: LocalDateTime = LocalDateTime.now()): FirebaseExperiment {
        return this.copy(
            lastNotificationSent = Timestamp(
                java.util.Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant())
            )
        )
    }
    
    /**
     * Update last logged at timestamp
     */
    @Exclude
    fun updateLastLoggedAt(timestamp: LocalDateTime = LocalDateTime.now()): FirebaseExperiment {
        return this.copy(
            lastLoggedAt = Timestamp(
                java.util.Date.from(timestamp.atZone(ZoneId.systemDefault()).toInstant())
            )
        )
    }
} 