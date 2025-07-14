package com.nof1.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nof1.Nof1Application
import com.nof1.data.model.ReminderEntityType

/**
 * WorkManager worker for handling reminder notifications.
 */
class ReminderNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminder_id")
        if (reminderId.isNullOrEmpty()) {
            return Result.failure()
        }

        return try {
            val application = applicationContext as Nof1Application
            val reminderRepository = application.reminderRepository
            val projectRepository = application.projectRepository
            val hypothesisRepository = application.hypothesisRepository
            
            val reminder = reminderRepository.getReminderById(reminderId)
            if (reminder != null && reminder.isEnabled) {
                when (ReminderEntityType.valueOf(reminder.entityType)) {
                    ReminderEntityType.PROJECT -> {
                        val project = projectRepository.getProjectById(reminder.entityId)
                        project?.let {
                            NotificationHelper.showCustomReminderNotification(
                                applicationContext,
                                reminder.title,
                                reminder.description,
                                ReminderEntityType.PROJECT,
                                reminder.entityId
                            )
                        }
                    }
                    ReminderEntityType.HYPOTHESIS -> {
                        val hypothesis = hypothesisRepository.getHypothesisById(reminder.entityId)
                        hypothesis?.let {
                            NotificationHelper.showCustomReminderNotification(
                                applicationContext,
                                reminder.title,
                                reminder.description,
                                ReminderEntityType.HYPOTHESIS,
                                reminder.entityId
                            )
                        }
                    }
                    ReminderEntityType.EXPERIMENT -> {
                        // For experiments, we could add this support later
                        NotificationHelper.showCustomReminderNotification(
                            applicationContext,
                            reminder.title,
                            reminder.description,
                            ReminderEntityType.EXPERIMENT,
                            reminder.entityId
                        )
                    }
                }
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}