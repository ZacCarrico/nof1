package com.nof1.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.ReminderSettings
import com.nof1.data.repository.ReminderRepository
import com.nof1.utils.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing reminder settings.
 * Updated to work with Firebase-only repositories.
 */
class ReminderViewModel(
    application: Application,
    private val reminderRepository: ReminderRepository
) : AndroidViewModel(application) {
    
    private val context: Context = application.applicationContext
    
    fun getReminderSettingsForEntity(entityType: String, entityId: String): Flow<List<ReminderSettings>> {
        return reminderRepository.getReminderSettingsForEntity(entityType, entityId)
    }
    
    fun getRemindersForProject(projectId: String): Flow<List<ReminderSettings>> {
        return reminderRepository.getRemindersForProject(projectId)
    }
    
    fun createReminder(reminderSettings: ReminderSettings) {
        viewModelScope.launch {
            val reminderId = reminderRepository.insertReminderSettings(reminderSettings)
            if (reminderId != null) {
                val updatedReminder = reminderSettings.copy(id = reminderId)
                ReminderScheduler.scheduleReminder(context, updatedReminder)
            }
        }
    }
    
    fun updateReminder(reminderSettings: ReminderSettings) {
        viewModelScope.launch {
            reminderRepository.updateReminderSettings(reminderSettings)
            ReminderScheduler.scheduleReminder(context, reminderSettings)
        }
    }
    
    fun deleteReminder(reminderSettings: ReminderSettings) {
        viewModelScope.launch {
            ReminderScheduler.cancelReminder(context, reminderSettings.id)
            reminderRepository.deleteReminderSettings(reminderSettings)
        }
    }
    
    fun toggleReminderEnabled(reminderId: String, isEnabled: Boolean) {
        viewModelScope.launch {
            val success = reminderRepository.toggleReminderEnabled(reminderId, isEnabled)
            if (success) {
                val reminder = reminderRepository.getReminderById(reminderId)
                reminder?.let {
                    if (isEnabled) {
                        ReminderScheduler.scheduleReminder(context, it)
                    } else {
                        ReminderScheduler.cancelReminder(context, reminderId)
                    }
                }
            }
        }
    }
    
    fun rescheduleAllReminders() {
        viewModelScope.launch {
            val allReminders = reminderRepository.getAllActiveReminders()
            ReminderScheduler.rescheduleAllReminders(context, allReminders)
        }
    }
}

/**
 * Factory for creating ReminderViewModel instances.
 */
class ReminderViewModelFactory(
    private val application: Application,
    private val reminderRepository: ReminderRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            return ReminderViewModel(application, reminderRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}