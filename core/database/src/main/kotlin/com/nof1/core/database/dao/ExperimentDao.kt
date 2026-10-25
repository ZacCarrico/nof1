package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.ExperimentEntity
import com.nof1.core.database.model.PopulatedExperiment
import com.nof1.core.database.model.QuestionEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ExperimentDao {
    @Transaction
    @Query("SELECT * FROM experiments WHERE hypothesis_id = :hypothesisId ORDER BY created_at, id")
    abstract fun observeForHypothesis(hypothesisId: String): Flow<List<PopulatedExperiment>>

    @Transaction
    @Query("SELECT * FROM experiments WHERE id = :id")
    abstract fun observe(id: String): Flow<PopulatedExperiment?>

    /** Experiments that have started and aren't archived, including finished ones. */
    @Transaction
    @Query(
        """
        SELECT * FROM experiments
        WHERE start_date IS NOT NULL AND archived_at IS NULL
        ORDER BY start_date, id
        """,
    )
    abstract fun observeStarted(): Flow<List<PopulatedExperiment>>

    /**
     * Saves the experiment and its questions, and removes questions it no longer has. Removing a
     * question that has answers fails, so a started experiment can't lose answers this way.
     */
    @Transaction
    open suspend fun save(experiment: ExperimentEntity, questions: List<QuestionEntity>) {
        require(questions.isNotEmpty()) {
            "Experiment ${experiment.id} needs at least one question"
        }
        require(questions.all { it.experimentId == experiment.id }) {
            "Questions must belong to experiment ${experiment.id}"
        }
        upsert(experiment)
        deleteQuestionsExcept(experiment.id, questions.map { it.id })
        upsertQuestions(questions)
    }

    /** Use [save], which also saves the questions. */
    @Upsert
    abstract suspend fun upsert(experiment: ExperimentEntity)

    @Upsert
    abstract suspend fun upsertQuestions(questions: List<QuestionEntity>)

    @Query("DELETE FROM questions WHERE experiment_id = :experimentId AND id NOT IN (:keptIds)")
    abstract suspend fun deleteQuestionsExcept(experimentId: String, keptIds: List<String>)

    /** Deletes the experiment with its questions, check-ins and notes, and records the deletion. */
    @Transaction
    open suspend fun delete(id: String, deletedAt: Long) {
        if (deleteRow(id) > 0) {
            recordDeletion(DeletionEntity(DeletionEntity.EXPERIMENT, id, deletedAt))
        }
    }

    /** Use [delete], which also records the deletion. */
    @Query("DELETE FROM experiments WHERE id = :id")
    abstract suspend fun deleteRow(id: String): Int

    @Upsert
    abstract suspend fun recordDeletion(deletion: DeletionEntity)
}
