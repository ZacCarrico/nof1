package com.nof1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nof1.data.model.LogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for LogEntry entity.
 */
@Dao
interface LogEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(logEntry: LogEntry): Long

    @Update
    suspend fun update(logEntry: LogEntry)

    @Delete
    suspend fun delete(logEntry: LogEntry)

    @Query("SELECT * FROM log_entries WHERE id = :id")
    suspend fun getLogEntryById(id: Long): LogEntry?

    @Query("SELECT * FROM log_entries WHERE experimentId = :experimentId ORDER BY createdAt DESC")
    fun getLogEntriesForExperiment(experimentId: Long): Flow<List<LogEntry>>
    
    @Query("SELECT * FROM log_entries WHERE experimentId = :experimentId ORDER BY createdAt DESC")
    suspend fun getLogEntriesForExperimentSync(experimentId: Long): List<LogEntry>
    
    @Query("SELECT * FROM log_entries WHERE experimentId = :experimentId AND createdAt > :timestamp ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestLogEntryAfterTimestamp(experimentId: Long, timestamp: LocalDateTime): LogEntry?
    
    @Query("SELECT * FROM log_entries WHERE experimentId = :experimentId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestLogEntryForExperiment(experimentId: Long): LogEntry?
} 