package com.nof1.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.database.model.asEntity
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DeletionTest {
    private lateinit var db: Nof1Database

    @Before
    fun createDatabase() {
        db = inMemoryDatabase()
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    @Test
    fun deletingAProjectDeletesEverythingInItAndRecordsOnlyTheProject() = runTest {
        db.insertProjectTree()
        db.projectDao().upsert(project("other").asEntity())

        db.projectDao().delete("project", deletedAt = 1_000)

        assertEquals(1L, db.rowCount("projects"))
        for (table in listOf("hypotheses", "experiments", "questions", "check_ins", "answers")) {
            assertEquals(0L, db.rowCount(table), table)
        }
        assertEquals(0L, db.rowCount("notes"))
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.PROJECT, "project", 1_000)),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun deletingAHypothesisKeepsItsProjectAndTheProjectsNotes() = runTest {
        db.insertProjectTree()

        db.hypothesisDao().delete("hypothesis", deletedAt = 1_000)

        val snapshot = db.exportDao().snapshot()
        assertEquals(listOf("project"), snapshot.projects.map { it.id })
        assertEquals(emptyList(), snapshot.hypotheses)
        assertEquals(emptyList(), snapshot.experiments)
        assertEquals(listOf("project-note"), snapshot.notes.map { it.id })
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.HYPOTHESIS, "hypothesis", 1_000)),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun deletingAnExperimentDeletesItsQuestionsCheckInsAnswersAndNotes() = runTest {
        db.insertProjectTree()

        db.experimentDao().delete("experiment", deletedAt = 1_000)

        for (table in listOf("experiments", "questions", "check_ins", "answers")) {
            assertEquals(0L, db.rowCount(table), table)
        }
        assertEquals(
            setOf("project-note", "hypothesis-note"),
            db.exportDao().snapshot().notes.map { it.id }.toSet(),
        )
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.EXPERIMENT, "experiment", 1_000)),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun deletingACheckInDeletesItsAnswers() = runTest {
        db.insertProjectTree()

        db.checkInDao().delete("check-in", deletedAt = 1_000)

        assertEquals(0L, db.rowCount("check_ins"))
        assertEquals(0L, db.rowCount("answers"))
        assertEquals(3L, db.rowCount("questions"))
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.CHECK_IN, "check-in", 1_000)),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun deletingANoteRecordsIt() = runTest {
        db.insertProjectTree()

        db.noteDao().delete("experiment-note", deletedAt = 1_000)

        assertEquals(2L, db.rowCount("notes"))
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.NOTE, "experiment-note", 1_000)),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun deletingSomethingMissingRecordsNothing() = runTest {
        db.projectDao().delete("missing", deletedAt = 1_000)
        db.checkInDao().delete("missing", deletedAt = 1_000)

        assertEquals(emptyList(), db.deletionDao().getAll())
    }
}
