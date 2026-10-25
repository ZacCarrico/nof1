package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.HypothesisEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class HypothesisDao {
    @Query("SELECT * FROM hypotheses WHERE project_id = :projectId ORDER BY created_at, id")
    abstract fun observeForProject(projectId: String): Flow<List<HypothesisEntity>>

    @Query("SELECT * FROM hypotheses WHERE id = :id")
    abstract fun observe(id: String): Flow<HypothesisEntity?>

    /** Inserts the hypothesis, or updates it if it exists. */
    @Upsert
    abstract suspend fun upsert(hypothesis: HypothesisEntity)

    /** Deletes the hypothesis with its experiments and notes, and records the deletion. */
    @Transaction
    open suspend fun delete(id: String, deletedAt: Long) {
        if (deleteRow(id) > 0) {
            recordDeletion(DeletionEntity(DeletionEntity.HYPOTHESIS, id, deletedAt))
        }
    }

    /** Use [delete], which also records the deletion. */
    @Query("DELETE FROM hypotheses WHERE id = :id")
    abstract suspend fun deleteRow(id: String): Int

    @Upsert
    abstract suspend fun recordDeletion(deletion: DeletionEntity)
}
