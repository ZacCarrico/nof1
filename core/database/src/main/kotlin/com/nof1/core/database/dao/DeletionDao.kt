package com.nof1.core.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import com.nof1.core.database.model.DeletionEntity

/** Reads the deletion records; each DAO writes its own when it deletes. */
@Dao
interface DeletionDao {
    @Query("SELECT * FROM deletions ORDER BY deleted_at, entity_type, entity_id")
    suspend fun getAll(): List<DeletionEntity>
}
