package com.nof1.data.repository

import com.nof1.data.local.NoteDao
import com.nof1.data.model.Note
import com.nof1.utils.ImageUtils
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
        // Get the old note to check if we need to clean up old image
        val oldNote = noteDao.getNoteById(note.id)
        val updatedNote = note.copy(updatedAt = LocalDateTime.now())
        noteDao.update(updatedNote)
        
        // Clean up old image if it was replaced with a different one
        oldNote?.imagePath?.let { oldImagePath ->
            if (oldImagePath != note.imagePath) {
                ImageUtils.deleteImage(oldImagePath)
            }
        }
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
        // Get all notes first to clean up their images
        val notes = noteDao.getNotesForHypothesisSync(hypothesisId)
        notes.forEach { note ->
            note.imagePath?.let { imagePath ->
                ImageUtils.deleteImage(imagePath)
            }
        }
        noteDao.deleteAllNotesForHypothesis(hypothesisId)
    }
} 