package com.nof1.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.nof1.core.database.model.asExternalModel
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNull
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExperimentDaoTest {
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
    fun savedExperimentReadsBackEqual() = runTest {
        db.insertProjectAndHypothesis()
        val experiment = experiment()

        db.save(experiment)

        assertEquals(
            experiment,
            db.experimentDao().observe("experiment").first()?.asExternalModel(),
        )
    }

    @Test
    fun savingADraftWithFewerQuestionsRemovesTheDroppedOnes() = runTest {
        db.insertProjectAndHypothesis()
        val draft = experiment(started = false)
        db.save(draft)

        val edited = draft.copy(secondaryQuestions = listOf(remarksQuestion()))
        db.save(edited)

        assertEquals(edited, db.experimentDao().observe("experiment").first()?.asExternalModel())
        assertEquals(2L, db.rowCount("questions"))
    }

    @Test
    fun removingAQuestionThatHasAnswersFailsAndChangesNothing() = runTest {
        db.insertProjectAndHypothesis()
        val experiment = experiment()
        db.save(experiment)
        db.save(checkIn())

        assertFails {
            db.save(
                experiment.copy(title = "Renamed", secondaryQuestions = listOf(remarksQuestion())),
            )
        }

        assertEquals(
            experiment,
            db.experimentDao().observe("experiment").first()?.asExternalModel(),
        )
        assertEquals(3L, db.rowCount("answers"))
    }

    @Test
    fun observeStartedListsStartedExperimentsThatArentArchived() = runTest {
        db.insertProjectAndHypothesis()
        db.save(experiment(id = "draft", started = false))
        db.save(experiment(id = "running"))
        db.save(experiment(id = "archived").copy(archivedAt = UPDATED))

        val started = db.experimentDao().observeStarted().first()

        assertEquals(listOf("running"), started.map { it.experiment.id })
    }

    @Test
    fun observeEmitsEachSavedVersion() = runTest {
        db.insertProjectAndHypothesis()
        val draft = experiment(started = false)
        val started = experiment(started = true)

        db.experimentDao().observe("experiment")
            .map { it?.asExternalModel() }
            .distinctUntilChanged()
            .test {
                assertNull(awaitItem())
                db.save(draft)
                assertEquals(draft, awaitItem())
                db.save(started)
                assertEquals(started, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
    }
}
