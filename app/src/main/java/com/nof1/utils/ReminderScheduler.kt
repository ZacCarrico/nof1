package com.nof1.utils

import android.content.Context
import androidx.work.*
import com.nof1.data.model.ReminderSettings
import com.nof1.data.model.ReminderFrequency
import com.nof1.data.model.DayOfWeek
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/**
 * Utility class for scheduling custom reminder notifications using WorkManager.
 */
object ReminderScheduler {
    
    // Minimum delay in seconds to prevent immediate notification execution
    // This prevents race conditions where very small delays cause immediate triggers
    private const val MIN_DELAY_SECONDS = 30
    
    fun scheduleReminder(context: Context, reminderSettings: ReminderSettings) {
        if (!reminderSettings.isEnabled) {
            cancelReminder(context, reminderSettings.id)
            return
        }
        
        when (reminderSettings.frequency) {
            ReminderFrequency.ONCE -> scheduleOneTimeReminder(context, reminderSettings)
            ReminderFrequency.DAILY -> scheduleDailyReminder(context, reminderSettings)
            ReminderFrequency.WEEKLY -> scheduleWeeklyReminder(context, reminderSettings)
            ReminderFrequency.MONTHLY -> scheduleMonthlyReminder(context, reminderSettings)
            ReminderFrequency.CUSTOM -> scheduleCustomReminder(context, reminderSettings)
        }
    }
    
    fun cancelReminder(context: Context, reminderId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("reminder_$reminderId")
    }
    
    fun rescheduleAllReminders(context: Context, allReminders: List<ReminderSettings>) {
        allReminders.forEach { reminder ->
            if (reminder.isEnabled) {
                scheduleReminder(context, reminder)
            }
        }
    }
    
    private fun scheduleOneTimeReminder(context: Context, reminderSettings: ReminderSettings) {
        val data = createWorkData(reminderSettings)
        val initialDelay = calculateInitialDelay(reminderSettings.time)
        
        val workRequest = OneTimeWorkRequestBuilder<ReminderNotificationWorker>()
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                "reminder_${reminderSettings.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
    }
    
    private fun scheduleDailyReminder(context: Context, reminderSettings: ReminderSettings) {
        val data = createWorkData(reminderSettings)
        val initialDelay = calculateInitialDelay(reminderSettings.time)
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(1, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "reminder_${reminderSettings.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    private fun scheduleWeeklyReminder(context: Context, reminderSettings: ReminderSettings) {
        val data = createWorkData(reminderSettings)
        val initialDelay = calculateWeeklyInitialDelay(reminderSettings.time, reminderSettings.daysOfWeek)
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(7, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "reminder_${reminderSettings.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    private fun scheduleMonthlyReminder(context: Context, reminderSettings: ReminderSettings) {
        val data = createWorkData(reminderSettings)
        val initialDelay = calculateMonthlyInitialDelay(reminderSettings.time)
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(30, TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "reminder_${reminderSettings.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    private fun scheduleCustomReminder(context: Context, reminderSettings: ReminderSettings) {
        val data = createWorkData(reminderSettings)
        val initialDelay = calculateInitialDelay(reminderSettings.time)
        val customDays = reminderSettings.customFrequencyDays ?: 1
        
        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(customDays.toLong(), TimeUnit.DAYS)
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(createConstraints())
            .build()
        
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "reminder_${reminderSettings.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    private fun createWorkData(reminderSettings: ReminderSettings): Data {
        return Data.Builder()
            .putLong("reminder_id", reminderSettings.id)
            .build()
    }
    
    private fun createConstraints(): Constraints {
        return Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()
    }
    
    private fun calculateInitialDelay(notificationTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val today = now.toLocalDate()
        val todayAtNotificationTime = today.atTime(notificationTime)
        
        val targetTime = if (todayAtNotificationTime.isAfter(now)) {
            todayAtNotificationTime
        } else {
            todayAtNotificationTime.plusDays(1)
        }
        
        val delay = Duration.between(now, targetTime).toMillis()
        
        // Prevent immediate notifications by enforcing minimum delay
        // If delay is less than MIN_DELAY_SECONDS, schedule for next occurrence
        return if (delay < MIN_DELAY_SECONDS * 1000) {
            // Schedule for tomorrow at the same time to prevent immediate execution
            val tomorrowAtNotificationTime = todayAtNotificationTime.plusDays(1)
            Duration.between(now, tomorrowAtNotificationTime).toMillis()
        } else {
            delay
        }
    }
    
    private fun calculateWeeklyInitialDelay(notificationTime: LocalTime, daysOfWeek: Set<DayOfWeek>): Long {
        if (daysOfWeek.isEmpty()) {
            return calculateInitialDelay(notificationTime)
        }
        
        val now = LocalDateTime.now()
        val currentDayOfWeek = now.dayOfWeek.value
        
        // Find the next occurrence of any of the specified days
        val nextDay = daysOfWeek.map { it.value }
            .filter { it >= currentDayOfWeek }
            .minOrNull()
            ?: (daysOfWeek.map { it.value }.minOrNull()!! + 7)
        
        val daysUntilNext = if (nextDay >= currentDayOfWeek) {
            nextDay - currentDayOfWeek
        } else {
            7 - (currentDayOfWeek - nextDay)
        }
        
        val targetTime = now.toLocalDate().plusDays(daysUntilNext.toLong()).atTime(notificationTime)
        val delay = Duration.between(now, targetTime).toMillis()
        
        // Prevent immediate notifications for weekly reminders too
        return if (delay < MIN_DELAY_SECONDS * 1000) {
            // If delay is too small, add a week to schedule for next occurrence
            val nextWeekTargetTime = targetTime.plusWeeks(1)
            Duration.between(now, nextWeekTargetTime).toMillis()
        } else {
            delay
        }
    }
    
    private fun calculateMonthlyInitialDelay(notificationTime: LocalTime): Long {
        val now = LocalDateTime.now()
        val currentDay = now.dayOfMonth
        val today = now.toLocalDate()
        val todayAtNotificationTime = today.atTime(notificationTime)
        
        val targetTime = if (todayAtNotificationTime.isAfter(now)) {
            todayAtNotificationTime
        } else {
            today.plusMonths(1).withDayOfMonth(currentDay).atTime(notificationTime)
        }
        
        val delay = Duration.between(now, targetTime).toMillis()
        
        // Prevent immediate notifications for monthly reminders too
        return if (delay < MIN_DELAY_SECONDS * 1000) {
            // If delay is too small, add a month to schedule for next occurrence
            val nextMonthTargetTime = targetTime.plusMonths(1)
            Duration.between(now, nextMonthTargetTime).toMillis()
        } else {
            delay
        }
    }
}