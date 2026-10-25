package com.nof1.core.data.repository

import com.nof1.core.model.Note
import com.nof1.core.model.NoteId
import com.nof1.core.model.NoteParent
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    /** The notes on a project, hypothesis or experiment, oldest first. */
    fun observeFor(parent: NoteParent): Flow<List<Note>>

    /** Adds the note, or replaces the stored one with the same ID. */
    suspend fun save(note: Note)

    suspend fun delete(id: NoteId)
}
