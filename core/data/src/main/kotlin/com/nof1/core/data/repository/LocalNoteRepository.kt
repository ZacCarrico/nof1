package com.nof1.core.data.repository

import com.nof1.core.database.dao.NoteDao
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.asExternalModel
import com.nof1.core.model.Note
import com.nof1.core.model.NoteId
import com.nof1.core.model.NoteParent
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class LocalNoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val clock: Clock,
) : NoteRepository {
    override fun observeFor(parent: NoteParent): Flow<List<Note>> {
        val notes = when (parent) {
            is NoteParent.OfProject -> noteDao.observeForProject(parent.projectId.value)
            is NoteParent.OfHypothesis -> noteDao.observeForHypothesis(parent.hypothesisId.value)
            is NoteParent.OfExperiment -> noteDao.observeForExperiment(parent.experimentId.value)
        }
        return notes.map { entities -> entities.map { it.asExternalModel() } }
    }

    override suspend fun save(note: Note) = noteDao.upsert(note.asEntity())

    override suspend fun delete(id: NoteId) = noteDao.delete(id.value, clock.millis())
}
