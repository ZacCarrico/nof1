package com.nof1.utils

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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

    fun showExperimentNotification(context: Context, experimentId: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("experiment_id", experimentId)
            putExtra("from_notification", true)
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            experimentId.hashCode(), 
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
        notificationManager.notify(experimentId.hashCode(), builder.build())
    }

    fun showCustomReminderNotification(
        context: Context, 
        title: String, 
        description: String,
        entityType: ReminderEntityType,
        entityId: String
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
                ReminderEntityType.EXPERIMENT -> {
                    putExtra("experiment_id", entityId)
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
    
    /**
     * Checks if notification permission is granted (for Android 13+).
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, notifications are enabled by default
            true
        }
    }
    
    /**
     * Checks if notifications are enabled for the app.
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }
} 