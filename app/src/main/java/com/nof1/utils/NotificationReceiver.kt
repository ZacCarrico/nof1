package com.nof1.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * BroadcastReceiver for handling notification actions.
 */
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("NotificationReceiver", "Received notification action: ${intent.action}")
        
        when (intent.action) {
            "EXPERIMENT_NOTIFICATION" -> {
                val experimentId = intent.getLongExtra("experiment_id", -1)
                if (experimentId != -1L) {
                    // Handle experiment notification
                    NotificationHelper.showExperimentNotification(context, experimentId)
                }
            }
        }
    }
} 