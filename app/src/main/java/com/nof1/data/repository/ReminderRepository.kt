package com.nof1.data.repository

import com.nof1.data.local.ReminderSettingsDao
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderSettings
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing reminder settings data.
 */
class ReminderRepository(private val reminderSettingsDao: ReminderSettingsDao) {
    
    fun getReminderSettingsForEntity(entityType: ReminderEntityType, entityId: Long): Flow<List<ReminderSettings>> {
        return reminderSettingsDao.getReminderSettingsForEntity(entityType, entityId)
    }
    
    suspend fun getReminderSettingsForEntitySync(entityType: ReminderEntityType, entityId: Long): List<ReminderSettings> {
        return reminderSettingsDao.getReminderSettingsForEntitySync(entityType, entityId)
    }
    
    suspend fun getAllActiveReminders(): List<ReminderSettings> {
        return reminderSettingsDao.getAllActiveReminders()
    }
    
    suspend fun getReminderById(id: Long): ReminderSettings? {
        return reminderSettingsDao.getReminderById(id)
    }
    
    suspend fun insertReminderSettings(reminderSettings: ReminderSettings): Long {
        return reminderSettingsDao.insertReminderSettings(reminderSettings)
    }
    
    suspend fun insertAllReminderSettings(reminderSettings: List<ReminderSettings>) {
        reminderSettingsDao.insertAllReminderSettings(reminderSettings)
    }
    
    suspend fun updateReminderSettings(reminderSettings: ReminderSettings) {
        reminderSettingsDao.updateReminderSettings(reminderSettings)
    }
    
    suspend fun deleteReminderSettings(reminderSettings: ReminderSettings) {
        reminderSettingsDao.deleteReminderSettings(reminderSettings)
    }
    
    suspend fun deleteAllRemindersForEntity(entityType: ReminderEntityType, entityId: Long) {
        reminderSettingsDao.deleteAllRemindersForEntity(entityType, entityId)
    }
    
    suspend fun deleteReminderById(id: Long) {
        reminderSettingsDao.deleteReminderById(id)
    }
    
    suspend fun toggleReminderEnabled(id: Long, isEnabled: Boolean) {
        reminderSettingsDao.toggleReminderEnabled(id, isEnabled)
    }
}