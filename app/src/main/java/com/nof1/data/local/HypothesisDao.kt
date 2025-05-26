package com.nof1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.nof1.data.model.Hypothesis
import com.nof1.data.model.HypothesisWithExperiments
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Hypothesis entity.
 */
@Dao
interface HypothesisDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hypothesis: Hypothesis): Long

    @Update
    suspend fun update(hypothesis: Hypothesis)

    @Delete
    suspend fun delete(hypothesis: Hypothesis)

    @Query("SELECT * FROM hypotheses WHERE id = :id")
    suspend fun getHypothesisById(id: Long): Hypothesis?

    @Query("SELECT * FROM hypotheses WHERE projectId = :projectId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveHypothesesForProject(projectId: Long): Flow<List<Hypothesis>>

    @Query("SELECT * FROM hypotheses WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getAllHypothesesForProject(projectId: Long): Flow<List<Hypothesis>>

    @Transaction
    @Query("SELECT * FROM hypotheses WHERE id = :hypothesisId")
    fun getHypothesisWithExperiments(hypothesisId: Long): Flow<HypothesisWithExperiments?>

    @Transaction
    @Query("SELECT * FROM hypotheses WHERE projectId = :projectId AND isArchived = 0 ORDER BY updatedAt DESC")
    fun getActiveHypothesesWithExperimentsForProject(projectId: Long): Flow<List<HypothesisWithExperiments>>

    @Transaction
    @Query("SELECT * FROM hypotheses WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun getAllHypothesesWithExperimentsForProject(projectId: Long): Flow<List<HypothesisWithExperiments>>
} 