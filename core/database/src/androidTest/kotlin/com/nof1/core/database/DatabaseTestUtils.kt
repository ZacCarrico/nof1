package com.nof1.core.database

import androidx.room3.useReaderConnection
import com.nof1.core.database.model.answerEntities
import com.nof1.core.database.model.asEntity
import com.nof1.core.database.model.questionEntities
import com.nof1.core.model.CheckIn
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.NoteParent
import com.nof1.core.model.ProjectId

suspend fun Nof1Database.save(experiment: Experiment) =
    experimentDao().save(experiment.asEntity(), experiment.questionEntities())

suspend fun Nof1Database.save(checkIn: CheckIn) =
    checkInDao().save(checkIn.asEntity(), checkIn.answerEntities())

/** A project with a hypothesis, ready for experiments. */
suspend fun Nof1Database.insertProjectAndHypothesis() {
    projectDao().upsert(project().asEntity())
    hypothesisDao().upsert(hypothesis().asEntity())
}

/** A project with one of everything under it, including a note on each level. */
suspend fun Nof1Database.insertProjectTree() {
    insertProjectAndHypothesis()
    save(experiment())
    save(checkIn())
    noteDao().upsert(note("project-note", NoteParent.OfProject(ProjectId("project"))).asEntity())
    noteDao().upsert(
        note("hypothesis-note", NoteParent.OfHypothesis(HypothesisId("hypothesis"))).asEntity(),
    )
    noteDao().upsert(
        note("experiment-note", NoteParent.OfExperiment(ExperimentId("experiment"))).asEntity(),
    )
}

/** The number of rows in [table], including tables no DAO reads directly. */
suspend fun Nof1Database.rowCount(table: String): Long = useReaderConnection { connection ->
    connection.usePrepared("SELECT COUNT(*) FROM $table") { statement ->
        statement.step()
        statement.getLong(0)
    }
}
