package com.nof1.data.repository

import com.nof1.data.local.LogEntryDao
import com.nof1.data.model.LogEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository for accessing LogEntry data.
 */
class LogEntryRepository(
    private val logEntryDao: LogEntryDao
) {
    suspend fun insertLogEntry(logEntry: LogEntry): Long {
        return logEntryDao.insert(logEntry)
    }

    suspend fun updateLogEntry(logEntry: LogEntry) {
        logEntryDao.update(logEntry)
    }

    suspend fun deleteLogEntry(logEntry: LogEntry) {
        logEntryDao.delete(logEntry)
    }

    suspend fun getLogEntryById(id: Long): LogEntry? {
        return logEntryDao.getLogEntryById(id)
    }

    fun getLogEntriesForExperiment(experimentId: Long): Flow<List<LogEntry>> {
        return logEntryDao.getLogEntriesForExperiment(experimentId)
    }

    suspend fun getLatestLogEntryAfterTimestamp(experimentId: Long, timestamp: LocalDateTime): LogEntry? {
        return logEntryDao.getLatestLogEntryAfterTimestamp(experimentId, timestamp)
    }

    suspend fun getLatestLogEntryForExperiment(experimentId: Long): LogEntry? {
        return logEntryDao.getLatestLogEntryForExperiment(experimentId)
    }
} 