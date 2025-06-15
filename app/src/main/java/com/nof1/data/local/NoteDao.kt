package com.nof1.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nof1.data.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Note entity.
 */
@Dao
interface NoteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE hypothesisId = :hypothesisId ORDER BY createdAt DESC")
    fun getNotesForHypothesis(hypothesisId: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE hypothesisId = :hypothesisId ORDER BY createdAt DESC")
    suspend fun getNotesForHypothesisSync(hypothesisId: Long): List<Note>

    @Query("DELETE FROM notes WHERE hypothesisId = :hypothesisId")
    suspend fun deleteAllNotesForHypothesis(hypothesisId: Long)
} 