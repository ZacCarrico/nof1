package com.nof1.utils

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.nof1.data.local.Nof1Database
import com.nof1.data.model.ReminderEntityType

/**
 * WorkManager worker for handling reminder notifications.
 */
class ReminderNotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong("reminder_id", -1L)
        if (reminderId == -1L) {
            return Result.failure()
        }

        return try {
            val database = Nof1Database.getDatabase(applicationContext)
            val reminderDao = database.reminderSettingsDao()
            
            val reminder = reminderDao.getReminderById(reminderId)
            if (reminder != null && reminder.isEnabled) {
                when (reminder.entityType) {
                    ReminderEntityType.PROJECT -> {
                        val project = database.projectDao().getProjectById(reminder.entityId)
                        project?.let {
                            NotificationHelper.showCustomReminderNotification(
                                applicationContext,
                                reminder.title,
                                reminder.description,
                                reminder.entityType,
                                reminder.entityId
                            )
                        }
                    }
                    ReminderEntityType.HYPOTHESIS -> {
                        val hypothesis = database.hypothesisDao().getHypothesisById(reminder.entityId)
                        hypothesis?.let {
                            NotificationHelper.showCustomReminderNotification(
                                applicationContext,
                                reminder.title,
                                reminder.description,
                                reminder.entityType,
                                reminder.entityId
                            )
                        }
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