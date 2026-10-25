package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProjectDao {
    @Query("SELECT * FROM projects ORDER BY created_at, id")
    abstract fun observeAll(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    abstract fun observe(id: String): Flow<ProjectEntity?>

    /** Inserts the project, or updates it if it exists. */
    @Upsert
    abstract suspend fun upsert(project: ProjectEntity)

    /** Deletes the project with everything in it, and records the deletion. */
    @Transaction
    open suspend fun delete(id: String, deletedAt: Long) {
        if (deleteRow(id) > 0) {
            recordDeletion(DeletionEntity(DeletionEntity.PROJECT, id, deletedAt))
        }
    }

    /** Use [delete], which also records the deletion. */
    @Query("DELETE FROM projects WHERE id = :id")
    abstract suspend fun deleteRow(id: String): Int

    @Upsert
    abstract suspend fun recordDeletion(deletion: DeletionEntity)
}
