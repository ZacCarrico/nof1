package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class NoteDao {
    @Query("SELECT * FROM notes WHERE project_id = :projectId ORDER BY created_at, id")
    abstract fun observeForProject(projectId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE hypothesis_id = :hypothesisId ORDER BY created_at, id")
    abstract fun observeForHypothesis(hypothesisId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE experiment_id = :experimentId ORDER BY created_at, id")
    abstract fun observeForExperiment(experimentId: String): Flow<List<NoteEntity>>

    /** Inserts the note, or updates it if it exists. */
    @Upsert
    abstract suspend fun upsert(note: NoteEntity)

    /** Deletes the note and records the deletion. */
    @Transaction
    open suspend fun delete(id: String, deletedAt: Long) {
        if (deleteRow(id) > 0) {
            recordDeletion(DeletionEntity(DeletionEntity.NOTE, id, deletedAt))
        }
    }

    /** Use [delete], which also records the deletion. */
    @Query("DELETE FROM notes WHERE id = :id")
    abstract suspend fun deleteRow(id: String): Int

    @Upsert
    abstract suspend fun recordDeletion(deletion: DeletionEntity)
}
