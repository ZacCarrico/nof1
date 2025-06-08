package com.nof1.data.local

import androidx.room.TypeConverter
import com.nof1.data.model.NotificationFrequency
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Type converters for Room database to handle custom types.
 */
class Converters {
    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val timeFormatter = DateTimeFormatter.ISO_LOCAL_TIME
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(dateTimeFormatter)
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(it, dateTimeFormatter) }
    }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? {
        return time?.format(timeFormatter)
    }

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? {
        return value?.let { LocalTime.parse(it, timeFormatter) }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(dateFormatter)
    }

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? {
        return value?.let { LocalDate.parse(it, dateFormatter) }
    }

    @TypeConverter
    fun fromNotificationFrequency(frequency: NotificationFrequency?): String? {
        return frequency?.name
    }

    @TypeConverter
    fun toNotificationFrequency(value: String?): NotificationFrequency? {
        return value?.let { NotificationFrequency.valueOf(it) }
    }

    @TypeConverter
    fun fromReminderFrequency(frequency: ReminderFrequency?): String? {
        return frequency?.name
    }

    @TypeConverter
    fun toReminderFrequency(value: String?): ReminderFrequency? {
        return value?.let { ReminderFrequency.valueOf(it) }
    }

    @TypeConverter
    fun fromReminderEntityType(entityType: ReminderEntityType?): String? {
        return entityType?.name
    }

    @TypeConverter
    fun toReminderEntityType(value: String?): ReminderEntityType? {
        return value?.let { ReminderEntityType.valueOf(it) }
    }

    @TypeConverter
    fun fromDaysOfWeekSet(daysOfWeek: Set<DayOfWeek>?): String? {
        return daysOfWeek?.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toDaysOfWeekSet(value: String?): Set<DayOfWeek>? {
        return value?.split(",")?.mapNotNull { 
            try { DayOfWeek.valueOf(it.trim()) } catch (e: Exception) { null }
        }?.toSet()
    }
} 