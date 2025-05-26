package com.nof1.utils

import android.content.Context
import androidx.work.*
import com.nof1.data.model.Experiment
import com.nof1.data.model.NotificationFrequency
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/**
 * Utility class for scheduling experiment notifications using WorkManager.
 */
object NotificationScheduler {
    
    fun scheduleExperimentNotification(context: Context, experiment: Experiment) {
        if (!experiment.notificationsEnabled || experiment.isArchived) {
            cancelExperimentNotification(context, experiment.id)
            return
        }
        
        val workRequest = createWorkRequest(experiment)
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "experiment_${experiment.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
    }
    
    fun cancelExperimentNotification(context: Context, experimentId: Long) {
        WorkManager.getInstance(context)
            .cancelUniqueWork("experiment_$experimentId")
    }
    
    fun rescheduleAllNotifications(context: Context) {
        // This would typically be called from the Application class or a service
        // to reschedule all active experiment notifications
        // For now, we'll leave this as a placeholder
    }
    
    private fun createWorkRequest(experiment: Experiment): PeriodicWorkRequest {
        val data = Data.Builder()
            .putLong("experiment_id", experiment.id)
            .build()
        
        val repeatInterval = when (experiment.notificationFrequency) {
            NotificationFrequency.DAILY -> 1L to TimeUnit.DAYS
            NotificationFrequency.WEEKLY -> 7L to TimeUnit.DAYS
            NotificationFrequency.CUSTOM -> {
                val days = experiment.customFrequencyDays ?: 1
                days.toLong() to TimeUnit.DAYS
            }
        }
        
        val initialDelay = calculateInitialDelay(experiment.notificationTime)
        
        return PeriodicWorkRequestBuilder<ExperimentNotificationWorker>(
            repeatInterval.first,
            repeatInterval.second
        )
            .setInputData(data)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
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
        
        return Duration.between(now, targetTime).toMillis()
    }
} 