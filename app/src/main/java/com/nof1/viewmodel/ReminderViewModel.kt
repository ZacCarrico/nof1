package com.nof1.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderSettings
import com.nof1.data.repository.ReminderRepository
import com.nof1.utils.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing reminder settings.
 */
class ReminderViewModel(
    private val reminderRepository: ReminderRepository,
    private val context: Context
) : ViewModel() {
    
    fun getReminderSettingsForEntity(entityType: ReminderEntityType, entityId: Long): Flow<List<ReminderSettings>> {
        return reminderRepository.getReminderSettingsForEntity(entityType, entityId)
    }
    
    fun createReminder(reminderSettings: ReminderSettings) {
        viewModelScope.launch {
            val reminderId = reminderRepository.insertReminderSettings(reminderSettings)
            val updatedReminder = reminderSettings.copy(id = reminderId)
            ReminderScheduler.scheduleReminder(context, updatedReminder)
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
    
    fun toggleReminderEnabled(reminderId: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            reminderRepository.toggleReminderEnabled(reminderId, isEnabled)
            val reminder = reminderRepository.getReminderById(reminderId)
            reminder?.let {
                val updatedReminder = it.copy(isEnabled = isEnabled)
                if (isEnabled) {
                    ReminderScheduler.scheduleReminder(context, updatedReminder)
                } else {
                    ReminderScheduler.cancelReminder(context, reminderId)
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
    private val reminderRepository: ReminderRepository,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            return ReminderViewModel(reminderRepository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}