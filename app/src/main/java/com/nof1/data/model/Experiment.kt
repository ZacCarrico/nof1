package com.nof1.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Represents an experiment within a hypothesis.
 * An experiment is a specific test to validate or refute a hypothesis.
 */
@Entity(
    tableName = "experiments",
    foreignKeys = [
        ForeignKey(
            entity = Hypothesis::class,
            parentColumns = ["id"],
            childColumns = ["hypothesisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hypothesisId")]
)
data class Experiment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val hypothesisId: Long,
    val projectId: Long? = null, // For Firebase syncing, nullable for backwards compatibility
    val name: String,
    val description: String,
    val question: String,
    
    // Notification settings
    val notificationsEnabled: Boolean = true,
    val notificationFrequency: NotificationFrequency = NotificationFrequency.DAILY,
    val notificationTime: LocalTime = LocalTime.of(9, 0), // Default to 9:00 AM
    val customFrequencyDays: Int? = null, // Used only when frequency is CUSTOM
    
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val lastNotificationSent: LocalDateTime? = null,
    val lastLoggedAt: LocalDateTime? = null
)

/**
 * Represents the frequency of notifications for an experiment.
 */
enum class NotificationFrequency {
    DAILY, WEEKLY, CUSTOM
} 