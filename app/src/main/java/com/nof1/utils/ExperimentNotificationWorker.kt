package com.nof1.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nof1.Nof1Application
import com.nof1.data.repository.ExperimentRepository
import com.nof1.data.repository.LogEntryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * WorkManager worker for handling experiment notifications.
 */
class ExperimentNotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val experimentId = inputData.getString("experiment_id")
            if (experimentId.isNullOrEmpty()) {
                return@withContext Result.failure()
            }

            val application = applicationContext as Nof1Application
            val experimentRepository = application.experimentRepository
            val logEntryRepository = application.logEntryRepository

            val experiment = experimentRepository.getExperimentById(experimentId)
            if (experiment == null || experiment.isArchived || !experiment.notificationsEnabled) {
                return@withContext Result.success()
            }

            // Check if user has logged manually since last notification
            val lastLogEntry = logEntryRepository.getLatestLogEntryForExperiment(experimentId)
            val lastNotificationTime = experiment.lastNotificationSent ?: experiment.createdAt

            if (lastLogEntry != null && lastLogEntry.createdAt != null && lastNotificationTime != null) {
                val lastLogTime = lastLogEntry.createdAt!!.toDate()
                val lastNotificationDate = lastNotificationTime.toDate()
                if (lastLogTime.after(lastNotificationDate)) {
                    // User has logged manually since last notification, skip this notification
                    return@withContext Result.success()
                }
            }

            // Show notification
            NotificationHelper.showExperimentNotification(applicationContext, experimentId)

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
} 