package com.nof1.data.local

import androidx.room.*
import com.nof1.data.model.ReminderEntityType
import com.nof1.data.model.ReminderSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for ReminderSettings.
 */
@Dao
interface ReminderSettingsDao {
    
    @Query("SELECT * FROM reminder_settings WHERE entityType = :entityType AND entityId = :entityId")
    fun getReminderSettingsForEntity(entityType: ReminderEntityType, entityId: Long): Flow<List<ReminderSettings>>
    
    @Query("SELECT * FROM reminder_settings WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getReminderSettingsForEntitySync(entityType: ReminderEntityType, entityId: Long): List<ReminderSettings>
    
    @Query("SELECT * FROM reminder_settings WHERE isEnabled = 1")
    suspend fun getAllActiveReminders(): List<ReminderSettings>
    
    @Query("SELECT * FROM reminder_settings WHERE id = :id")
    suspend fun getReminderById(id: Long): ReminderSettings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderSettings(reminderSettings: ReminderSettings): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReminderSettings(reminderSettings: List<ReminderSettings>)
    
    @Update
    suspend fun updateReminderSettings(reminderSettings: ReminderSettings)
    
    @Delete
    suspend fun deleteReminderSettings(reminderSettings: ReminderSettings)
    
    @Query("DELETE FROM reminder_settings WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteAllRemindersForEntity(entityType: ReminderEntityType, entityId: Long)
    
    @Query("DELETE FROM reminder_settings WHERE id = :id")
    suspend fun deleteReminderById(id: Long)
    
    @Query("UPDATE reminder_settings SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun toggleReminderEnabled(id: Long, isEnabled: Boolean)
}