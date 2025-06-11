package com.nof1.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class for managing app preferences and settings.
 */
class PreferencesHelper(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PREFS_NAME = "nof1_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"
    }
    
    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }
    
    fun setFirstLaunchCompleted() {
        prefs.edit()
            .putBoolean(KEY_FIRST_LAUNCH, false)
            .apply()
    }
    
    fun isNotificationPermissionRequested(): Boolean {
        return prefs.getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)
    }
    
    fun setNotificationPermissionRequested() {
        prefs.edit()
            .putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true)
            .apply()
    }
} 