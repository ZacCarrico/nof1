package com.nof1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nof1.data.model.Experiment
import com.nof1.data.model.ExperimentWithLogs
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for Experiment entity.
 */
@Dao
interface ExperimentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(experiment: Experiment): Long

    @Update
    suspend fun update(experiment: Experiment)

    @Delete
    suspend fun delete(experiment: Experiment)

    @Query("SELECT * FROM experiments WHERE id = :id")
    suspend fun getExperimentById(id: Long): Experiment?

    @Query("SELECT * FROM experiments WHERE hypothesisId = :hypothesisId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveExperimentsForHypothesis(hypothesisId: Long): Flow<List<Experiment>>

    @Query("SELECT * FROM experiments WHERE hypothesisId = :hypothesisId ORDER BY updatedAt DESC")
    fun getAllExperimentsForHypothesis(hypothesisId: Long): Flow<List<Experiment>>
    
    @Query("SELECT * FROM experiments WHERE notificationsEnabled = 1 AND isArchived = 0")
    fun getExperimentsWithNotificationsEnabled(): Flow<List<Experiment>>
    
    @Query("UPDATE experiments SET lastNotificationSent = :timestamp WHERE id = :experimentId")
    suspend fun updateLastNotificationSent(experimentId: Long, timestamp: LocalDateTime)
    
    @Query("UPDATE experiments SET lastLoggedAt = :timestamp WHERE id = :experimentId")
    suspend fun updateLastLoggedAt(experimentId: Long, timestamp: LocalDateTime)

    @Transaction
    @Query("SELECT * FROM experiments WHERE id = :experimentId")
    fun getExperimentWithLogs(experimentId: Long): Flow<ExperimentWithLogs?>

    @Transaction
    @Query("SELECT * FROM experiments WHERE hypothesisId = :hypothesisId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveExperimentsWithLogsForHypothesis(hypothesisId: Long): Flow<List<ExperimentWithLogs>>
} 