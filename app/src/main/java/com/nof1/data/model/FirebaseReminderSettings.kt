package com.nof1.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import java.time.LocalTime

/**
 * Firebase-compatible version of ReminderSettings for Firestore storage.
 */
data class FirebaseReminderSettings(
    @DocumentId
    val id: String = "",
    
    val entityType: String = "", // "PROJECT" or "HYPOTHESIS"
    val entityId: String = "", // Firebase document ID of the entity
    val projectId: String = "", // For easier querying
    val isEnabled: Boolean = true,
    val title: String = "",
    val description: String = "",
    val frequency: String = "DAILY", // "ONCE", "DAILY", "WEEKLY", "MONTHLY", "CUSTOM"
    val timeHour: Int = 9,
    val timeMinute: Int = 0,
    val customFrequencyDays: Int? = null,
    val daysOfWeek: List<String> = emptyList(), // ["MONDAY", "TUESDAY", etc.]
    val endDate: String? = null, // ISO date string
    val userId: String = "",
    
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    
    @ServerTimestamp
    val updatedAt: Timestamp? = null
) {
    // No-argument constructor required by Firestore
    constructor() : this("", "", "", "", true, "", "", "DAILY", 9, 0, null, emptyList(), null, "", null, null)
    
    /**
     * Get the notification time as LocalTime
     */
    @Exclude
    fun getNotificationTime(): LocalTime {
        return LocalTime.of(timeHour, timeMinute)
    }
    
    /**
     * Get the days of week as a Set of DayOfWeek enum values
     */
    @Exclude
    fun toDaysOfWeekSet(): Set<DayOfWeek> {
        return daysOfWeek.mapNotNull { dayString ->
            try {
                DayOfWeek.valueOf(dayString)
            } catch (e: IllegalArgumentException) {
                null
            }
        }.toSet()
    }
    
    /**
     * Update the notification time
     */
    fun updateNotificationTime(localTime: LocalTime): FirebaseReminderSettings {
        return copy(
            timeHour = localTime.hour,
            timeMinute = localTime.minute
        )
    }
    
    /**
     * Update the days of week
     */
    fun updateDaysOfWeek(daysOfWeek: Set<DayOfWeek>): FirebaseReminderSettings {
        return copy(
            daysOfWeek = daysOfWeek.map { it.name }
        )
    }
    
    /**
     * Create a copy with updated timestamp
     */
    fun copyWithUpdatedTimestamp(): FirebaseReminderSettings {
        return copy(updatedAt = Timestamp.now())
    }
} 