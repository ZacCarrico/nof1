package com.nof1.core.data

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.nof1.core.data.repository.LocalCheckInRepository
import com.nof1.core.data.repository.LocalExperimentRepository
import com.nof1.core.data.repository.LocalExportRepository
import com.nof1.core.data.repository.LocalHypothesisRepository
import com.nof1.core.data.repository.LocalNoteRepository
import com.nof1.core.data.repository.LocalProjectRepository
import com.nof1.core.database.Nof1Database
import com.nof1.core.database.model.DeletionEntity
import com.nof1.core.model.Answer
import com.nof1.core.model.AnswerType
import com.nof1.core.model.BetterDirection
import com.nof1.core.model.CheckIn
import com.nof1.core.model.CheckInDay
import com.nof1.core.model.CheckInId
import com.nof1.core.model.ConditionDescription
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentDesign
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.Hypothesis
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.Note
import com.nof1.core.model.NoteId
import com.nof1.core.model.NoteParent
import com.nof1.core.model.PairOrder
import com.nof1.core.model.PrimaryOutcome
import com.nof1.core.model.Project
import com.nof1.core.model.ProjectId
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId
import com.nof1.core.model.Schedule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The repositories against a real (in-memory) database. */
@RunWith(AndroidJUnit4::class)
class RepositoriesTest {
    private val now = Instant.parse("2026-10-25T03:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    private lateinit var db: Nof1Database
    private lateinit var projects: LocalProjectRepository
    private lateinit var hypotheses: LocalHypothesisRepository
    private lateinit var experiments: LocalExperimentRepository
    private lateinit var checkIns: LocalCheckInRepository
    private lateinit var notes: LocalNoteRepository

    @Before
    fun createRepositories() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder<Nof1Database>(context)
            .setDriver(BundledSQLiteDriver())
            .build()
        projects = LocalProjectRepository(db.projectDao(), clock)
        hypotheses = LocalHypothesisRepository(db.hypothesisDao(), clock)
        experiments = LocalExperimentRepository(db.experimentDao(), clock)
        checkIns = LocalCheckInRepository(db.checkInDao(), clock)
        notes = LocalNoteRepository(db.noteDao(), clock)
    }

    @After
    fun closeDatabase() {
        db.close()
    }

    private suspend fun saveAll() {
        projects.save(PROJECT)
        hypotheses.save(HYPOTHESIS)
        experiments.save(EXPERIMENT)
        checkIns.save(CHECK_IN)
        notes.save(NOTE)
    }

    @Test
    fun everythingReadsBackAsSaved() = runTest {
        saveAll()

        assertEquals(listOf(PROJECT), projects.observeAll().first())
        assertEquals(PROJECT, projects.observe(PROJECT.id).first())
        assertEquals(listOf(HYPOTHESIS), hypotheses.observeForProject(PROJECT.id).first())
        assertEquals(HYPOTHESIS, hypotheses.observe(HYPOTHESIS.id).first())
        assertEquals(listOf(EXPERIMENT), experiments.observeForHypothesis(HYPOTHESIS.id).first())
        assertEquals(EXPERIMENT, experiments.observe(EXPERIMENT.id).first())
        assertEquals(listOf(EXPERIMENT), experiments.observeStarted().first())
        assertEquals(listOf(CHECK_IN), checkIns.observeForExperiment(EXPERIMENT.id).first())
        assertEquals(listOf(NOTE), notes.observeFor(NOTE.parent).first())
    }

    @Test
    fun observedProjectsFollowChanges() = runTest {
        projects.observeAll().test {
            assertEquals(emptyList(), awaitItem())
            projects.save(PROJECT)
            assertEquals(listOf(PROJECT), awaitItem())
            val renamed = PROJECT.copy(name = "Sleep deeper", updatedAt = now)
            projects.save(renamed)
            assertEquals(listOf(renamed), awaitItem())
            projects.delete(PROJECT.id)
            assertEquals(emptyList(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun deletingAProjectDeletesItsContentsAndRecordsTheTime() = runTest {
        saveAll()

        projects.delete(PROJECT.id)

        assertNull(projects.observe(PROJECT.id).first())
        assertEquals(emptyList(), hypotheses.observeForProject(PROJECT.id).first())
        assertNull(experiments.observe(EXPERIMENT.id).first())
        assertEquals(emptyList(), checkIns.observeForExperiment(EXPERIMENT.id).first())
        assertEquals(emptyList(), notes.observeFor(NOTE.parent).first())
        assertEquals(
            listOf(DeletionEntity(DeletionEntity.PROJECT, PROJECT.id.value, now.toEpochMilli())),
            db.deletionDao().getAll(),
        )
    }

    @Test
    fun notesAreListedUnderTheirOwnParentOnly() = runTest {
        saveAll()
        val projectNote = NOTE.copy(
            id = NoteId("project-note"),
            parent = NoteParent.OfProject(PROJECT.id),
        )
        notes.save(projectNote)

        assertEquals(
            listOf(projectNote),
            notes.observeFor(NoteParent.OfProject(PROJECT.id)).first(),
        )
        assertEquals(emptyList(), notes.observeFor(NoteParent.OfHypothesis(HYPOTHESIS.id)).first())
        assertEquals(listOf(NOTE), notes.observeFor(NoteParent.OfExperiment(EXPERIMENT.id)).first())
    }

    @Test
    fun exportsContainWhatWasSaved() = runTest {
        saveAll()
        val export =
            LocalExportRepository(db.exportDao(), clock, StandardTestDispatcher(testScheduler))

        val json = export.exportJson()
        val csv = export.exportCheckInsCsv()

        assertTrue("\"exportedAt\": \"2026-10-25T03:00:00Z\"" in json, json)
        assertTrue("\"name\": \"Sleep better\"" in json, json)
        assertTrue("\"text\": \"Late coffee\"" in json, json)
        val lines = csv.trimEnd().lines()
        assertEquals(3, lines.size, csv)
        assertTrue(
            lines[1].startsWith("experiment,No screens after 10pm,2026-03-02,1,1,baseline,"),
            csv,
        )
    }

    private companion object {
        val CREATED: Instant = Instant.parse("2026-03-01T18:30:00Z")
        val SLEEP =
            Question(QuestionId("sleep"), "How rested do you feel?", AnswerType.Rating(1, 10))
        val NAP = Question(QuestionId("nap"), "Did you nap?", AnswerType.YesNo)

        val PROJECT =
            Project(ProjectId("project"), "Sleep better", "Wake up rested", CREATED, CREATED)
        val HYPOTHESIS = Hypothesis(
            id = HypothesisId("hypothesis"),
            projectId = PROJECT.id,
            statement = "Screens late at night make my sleep worse",
            createdAt = CREATED,
            updatedAt = CREATED,
        )
        val EXPERIMENT = Experiment(
            id = ExperimentId("experiment"),
            hypothesisId = HYPOTHESIS.id,
            title = "No screens after 10pm",
            baseline = ConditionDescription("Usual evenings"),
            intervention = ConditionDescription("No screens"),
            primaryOutcome = PrimaryOutcome(SLEEP, BetterDirection.HIGHER, 1.0),
            secondaryQuestions = listOf(NAP),
            design = ExperimentDesign(3, 1, 2, LocalTime.of(21, 0), CheckInDay.SAME_DAY),
            schedule = Schedule(
                startDate = LocalDate.of(2026, 3, 2),
                pairOrders = listOf(PairOrder.BASELINE_FIRST, PairOrder.INTERVENTION_FIRST),
                seed = 42,
            ),
            createdAt = CREATED,
            updatedAt = CREATED,
        )
        val CHECK_IN = CheckIn(
            id = CheckInId("check-in"),
            experimentId = EXPERIMENT.id,
            studyDay = LocalDate.of(2026, 3, 2),
            followedPlan = true,
            answers = mapOf(SLEEP.id to Answer.Rating(6), NAP.id to Answer.YesNo(false)),
            recordedAt = Instant.parse("2026-03-03T02:05:00Z"),
            zone = ZoneId.of("America/New_York"),
            updatedAt = Instant.parse("2026-03-03T02:05:00Z"),
        )
        val NOTE = Note(
            id = NoteId("note"),
            parent = NoteParent.OfExperiment(EXPERIMENT.id),
            text = "Late coffee",
            createdAt = CREATED,
            updatedAt = CREATED,
        )
    }
}
