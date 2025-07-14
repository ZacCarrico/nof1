package com.nof1.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nof1.Nof1Application
import com.nof1.data.model.Experiment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling device boot completion.
 * Reschedules all experiment notifications after device reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device boot completed, rescheduling notifications")
            
            val application = context.applicationContext as Nof1Application
            
            // Reschedule all active experiment notifications
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val experiments = application.experimentRepository.getExperimentsWithNotificationsEnabled().firstOrNull() ?: emptyList()
                    experiments.forEach { experiment: Experiment ->
                        if (experiment.notificationsEnabled && !experiment.isArchived) {
                            NotificationScheduler.scheduleExperimentNotification(context, experiment)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Error rescheduling experiment notifications", e)
                }
            }
            
            // Reschedule all active reminder notifications
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val reminders = application.reminderRepository.getAllActiveReminders()
                    ReminderScheduler.rescheduleAllReminders(context, reminders)
                } catch (e: Exception) {
                    Log.e("BootCompletedReceiver", "Error rescheduling reminder notifications", e)
                }
            }
        }
    }
} 