package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nof1.core.database.model.AnswerEntity
import com.nof1.core.database.model.CheckInEntity
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.PopulatedCheckIn
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CheckInDao {
    @Transaction
    @Query("SELECT * FROM check_ins WHERE experiment_id = :experimentId ORDER BY study_day")
    abstract fun observeForExperiment(experimentId: String): Flow<List<PopulatedCheckIn>>

    /**
     * Saves the check-in and replaces its answers. A study day has at most one check-in, so saving
     * a different check-in for a day that already has one fails.
     */
    @Transaction
    open suspend fun save(checkIn: CheckInEntity, answers: List<AnswerEntity>) {
        require(answers.all { it.checkInId == checkIn.id }) {
            "Answers must belong to check-in ${checkIn.id}"
        }
        val existingId = idForStudyDay(checkIn.experimentId, checkIn.studyDay)
        require(existingId == null || existingId == checkIn.id) {
            "Study day ${checkIn.studyDay} already has check-in $existingId"
        }
        upsert(checkIn)
        deleteAnswers(checkIn.id)
        insertAnswers(answers)
    }

    @Query("SELECT id FROM check_ins WHERE experiment_id = :experimentId AND study_day = :studyDay")
    abstract suspend fun idForStudyDay(experimentId: String, studyDay: String): String?

    /** Use [save], which also saves the answers. */
    @Upsert
    abstract suspend fun upsert(checkIn: CheckInEntity)

    @Query("DELETE FROM answers WHERE check_in_id = :checkInId")
    abstract suspend fun deleteAnswers(checkInId: String)

    @Insert
    abstract suspend fun insertAnswers(answers: List<AnswerEntity>)

    /** Deletes the check-in with its answers, and records the deletion. */
    @Transaction
    open suspend fun delete(id: String, deletedAt: Long) {
        if (deleteRow(id) > 0) {
            recordDeletion(DeletionEntity(DeletionEntity.CHECK_IN, id, deletedAt))
        }
    }

    /** Use [delete], which also records the deletion. */
    @Query("DELETE FROM check_ins WHERE id = :id")
    abstract suspend fun deleteRow(id: String): Int

    @Upsert
    abstract suspend fun recordDeletion(deletion: DeletionEntity)
}
