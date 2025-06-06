package com.nof1.data.repository

import com.nof1.data.local.NoteDao
import com.nof1.data.model.Note
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Repository for accessing Note data.
 */
class NoteRepository(
    private val noteDao: NoteDao
) {
    suspend fun insertNote(note: Note): Long {
        return noteDao.insert(note)
    }

    suspend fun updateNote(note: Note) {
        val updatedNote = note.copy(updatedAt = LocalDateTime.now())
        noteDao.update(updatedNote)
    }

    suspend fun deleteNote(note: Note) {
        noteDao.delete(note)
    }

    suspend fun getNoteById(id: Long): Note? {
        return noteDao.getNoteById(id)
    }

    fun getNotesForHypothesis(hypothesisId: Long): Flow<List<Note>> {
        return noteDao.getNotesForHypothesis(hypothesisId)
    }

    suspend fun deleteAllNotesForHypothesis(hypothesisId: Long) {
        noteDao.deleteAllNotesForHypothesis(hypothesisId)
    }
} 