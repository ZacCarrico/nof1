package com.nof1.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for handling device boot completion.
 * Reschedules all experiment notifications after device reboot.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device boot completed, rescheduling notifications")
            
            // TODO: Reschedule all active experiment notifications
            // This would typically involve:
            // 1. Getting all active experiments with notifications enabled
            // 2. Scheduling notifications for each experiment
            NotificationScheduler.rescheduleAllNotifications(context)
        }
    }
} 