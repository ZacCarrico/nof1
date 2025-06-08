package com.nof1.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.nof1.MainActivity
import com.nof1.R
import com.nof1.data.model.ReminderEntityType

/**
 * Helper class for managing notifications.
 */
object NotificationHelper {
    private const val CHANNEL_ID = "experiment_notifications"
    private const val CHANNEL_NAME = "Experiment Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications for experiment check-ins"
    
    private const val REMINDER_CHANNEL_ID = "reminder_notifications"
    private const val REMINDER_CHANNEL_NAME = "Reminder Notifications"
    private const val REMINDER_CHANNEL_DESCRIPTION = "Custom reminder notifications for projects and hypotheses"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // Create experiment notifications channel
            val experimentChannel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(experimentChannel)
            
            // Create reminder notifications channel
            val reminderChannel = NotificationChannel(REMINDER_CHANNEL_ID, REMINDER_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = REMINDER_CHANNEL_DESCRIPTION
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    fun showExperimentNotification(context: Context, experimentId: Long) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("experiment_id", experimentId)
            putExtra("from_notification", true)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            experimentId.toInt(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText("Time to log your experiment response")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(experimentId.toInt(), builder.build())
    }

    fun showCustomReminderNotification(
        context: Context, 
        title: String, 
        description: String,
        entityType: ReminderEntityType,
        entityId: Long
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            when (entityType) {
                ReminderEntityType.PROJECT -> {
                    putExtra("project_id", entityId)
                    putExtra("from_reminder", true)
                }
                ReminderEntityType.HYPOTHESIS -> {
                    putExtra("hypothesis_id", entityId)
                    putExtra("from_reminder", true)
                }
            }
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            "${entityType.name}_${entityId}".hashCode(), 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(description.ifEmpty { "Don't forget to check your ${entityType.name.lowercase()}" })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify("${entityType.name}_${entityId}".hashCode(), builder.build())
    }
} 