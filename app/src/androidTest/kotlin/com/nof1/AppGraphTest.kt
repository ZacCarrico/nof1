package com.nof1

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.nof1.core.data.repository.CheckInRepository
import com.nof1.core.data.repository.ExperimentRepository
import com.nof1.core.data.repository.ExportRepository
import com.nof1.core.data.repository.HypothesisRepository
import com.nof1.core.data.repository.NoteRepository
import com.nof1.core.data.repository.ProjectRepository
import com.nof1.core.database.Nof1Database
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
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Uses the repositories as the app gets them from Hilt, backed by the app's real database file, to
 * check the wiring between the modules.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class AppGraphTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var database: Nof1Database

    @Inject lateinit var sameDatabase: Nof1Database

    @Inject lateinit var projects: ProjectRepository

    @Inject lateinit var hypotheses: HypothesisRepository

    @Inject lateinit var experiments: ExperimentRepository

    @Inject lateinit var checkIns: CheckInRepository

    @Inject lateinit var notes: NoteRepository

    @Inject lateinit var export: ExportRepository

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun startWithoutData() {
        context.deleteDatabase(Nof1Database.FILE_NAME)
        hiltRule.inject()
    }

    @After
    fun deleteData() {
        if (::database.isInitialized) database.close()
        context.deleteDatabase(Nof1Database.FILE_NAME)
    }

    @Test
    fun savesReadsExportsAndDeletesThroughTheAppGraph() = runTest {
        projects.save(PROJECT)
        hypotheses.save(HYPOTHESIS)
        experiments.save(EXPERIMENT)
        checkIns.save(CHECK_IN)
        notes.save(NOTE)

        assertEquals(listOf(PROJECT), projects.observeAll().first())
        assertEquals(listOf(HYPOTHESIS), hypotheses.observeForProject(PROJECT.id).first())
        assertEquals(EXPERIMENT, experiments.observe(EXPERIMENT.id).first())
        assertEquals(listOf(CHECK_IN), checkIns.observeForExperiment(EXPERIMENT.id).first())
        assertEquals(listOf(NOTE), notes.observeFor(NOTE.parent).first())
        assertTrue("\"title\": \"No screens after 10pm\"" in export.exportJson())
        assertEquals(3, export.exportCheckInsCsv().trimEnd().lines().size)

        projects.delete(PROJECT.id)

        assertNull(projects.observe(PROJECT.id).first())
        assertNull(experiments.observe(EXPERIMENT.id).first())
    }

    @Test
    fun theAppHasOneDatabaseInstance() {
        assertSame(database, sameDatabase)
    }

    private companion object {
        val CREATED: Instant = Instant.parse("2026-03-01T18:30:00Z")
        val SLEEP = Question(QuestionId("sleep"), "How rested do you feel?", AnswerType.Rating())
        val NAP = Question(QuestionId("nap"), "Did you nap?", AnswerType.YesNo)

        val PROJECT = Project(
            id = ProjectId("project"),
            name = "Sleep better",
            goal = "Wake up rested",
            createdAt = CREATED,
            updatedAt = CREATED,
        )
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
