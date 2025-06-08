package com.nof1.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalTime

/**
 * Represents reminder notification settings for projects and hypotheses.
 * Similar to Google Calendar reminder functionality.
 */
@Entity(tableName = "reminder_settings")
data class ReminderSettings(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val entityType: ReminderEntityType,
    val entityId: Long,
    val isEnabled: Boolean = true,
    val title: String,
    val description: String = "",
    val frequency: ReminderFrequency = ReminderFrequency.DAILY,
    val time: LocalTime = LocalTime.of(9, 0),
    val customFrequencyDays: Int? = null,
    val daysOfWeek: Set<DayOfWeek> = emptySet(), // For weekly reminders
    val endDate: java.time.LocalDate? = null // Optional end date for reminders
)

/**
 * Represents the type of entity that has reminder settings.
 */
enum class ReminderEntityType {
    PROJECT, HYPOTHESIS
}

/**
 * Represents the frequency of reminder notifications.
 * Extended from the existing NotificationFrequency to include more options.
 */
enum class ReminderFrequency {
    ONCE,           // One-time reminder
    DAILY,          // Every day
    WEEKLY,         // Weekly on specific days
    MONTHLY,        // Monthly
    CUSTOM          // Custom interval in days
}

/**
 * Represents days of the week for weekly reminders.
 */
enum class DayOfWeek(val value: Int) {
    MONDAY(1),
    TUESDAY(2),
    WEDNESDAY(3),
    THURSDAY(4),
    FRIDAY(5),
    SATURDAY(6),
    SUNDAY(7)
}