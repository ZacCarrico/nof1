package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import com.nof1.core.database.model.HypothesisEntity
import com.nof1.core.database.model.NoteEntity
import com.nof1.core.database.model.PopulatedCheckIn
import com.nof1.core.database.model.PopulatedExperiment
import com.nof1.core.database.model.ProjectEntity

/** Everything the user has entered, read at one point in time. */
data class DatabaseSnapshot(
    val projects: List<ProjectEntity>,
    val hypotheses: List<HypothesisEntity>,
    val experiments: List<PopulatedExperiment>,
    val checkIns: List<PopulatedCheckIn>,
    val notes: List<NoteEntity>,
)

@Dao
abstract class ExportDao {
    /** Reads every table in one transaction, so the parts are consistent with each other. */
    @Transaction
    open suspend fun snapshot(): DatabaseSnapshot = DatabaseSnapshot(
        projects = projects(),
        hypotheses = hypotheses(),
        experiments = experiments(),
        checkIns = checkIns(),
        notes = notes(),
    )

    @Query("SELECT * FROM projects ORDER BY created_at, id")
    abstract suspend fun projects(): List<ProjectEntity>

    @Query("SELECT * FROM hypotheses ORDER BY created_at, id")
    abstract suspend fun hypotheses(): List<HypothesisEntity>

    @Transaction
    @Query("SELECT * FROM experiments ORDER BY created_at, id")
    abstract suspend fun experiments(): List<PopulatedExperiment>

    @Transaction
    @Query("SELECT * FROM check_ins ORDER BY experiment_id, study_day")
    abstract suspend fun checkIns(): List<PopulatedCheckIn>

    @Query("SELECT * FROM notes ORDER BY created_at, id")
    abstract suspend fun notes(): List<NoteEntity>
}
