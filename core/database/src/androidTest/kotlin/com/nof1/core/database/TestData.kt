package com.nof1.core.database

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** An empty in-memory database with the same SQLite driver as the app. */
fun inMemoryDatabase(): Nof1Database =
    Room.inMemoryDatabaseBuilder<Nof1Database>(ApplicationProvider.getApplicationContext<Context>())
        .setDriver(BundledSQLiteDriver())
        .build()

val CREATED: Instant = Instant.ofEpochMilli(1_772_400_000_000)
val UPDATED: Instant = Instant.ofEpochMilli(1_772_486_400_000)

// Question IDs are unique across experiments, as random IDs would be.

fun sleepQuestion(experimentId: String = "experiment") =
    Question(QuestionId("$experimentId-sleep"), "How rested do you feel?", AnswerType.Rating(1, 10))

fun napQuestion(experimentId: String = "experiment") =
    Question(QuestionId("$experimentId-nap"), "Did you nap?", AnswerType.YesNo)

fun remarksQuestion(experimentId: String = "experiment") =
    Question(QuestionId("$experimentId-remarks"), "Anything unusual?", AnswerType.FreeText)

fun project(id: String = "project") =
    Project(ProjectId(id), "Sleep better", "Fall asleep faster", CREATED, UPDATED)

fun hypothesis(id: String = "hypothesis", projectId: String = "project") = Hypothesis(
    id = HypothesisId(id),
    projectId = ProjectId(projectId),
    statement = "Screens late at night delay my sleep",
    createdAt = CREATED,
    updatedAt = UPDATED,
)

fun experiment(
    id: String = "experiment",
    hypothesisId: String = "hypothesis",
    started: Boolean = true,
) = Experiment(
    id = ExperimentId(id),
    hypothesisId = HypothesisId(hypothesisId),
    title = "No screens after 10pm",
    baseline = ConditionDescription("Usual evenings"),
    intervention = ConditionDescription("No screens", "Phone in the kitchen from 10pm"),
    primaryOutcome = PrimaryOutcome(sleepQuestion(id), BetterDirection.HIGHER, 1.0),
    secondaryQuestions = listOf(napQuestion(id), remarksQuestion(id)),
    design = ExperimentDesign(
        periodLengthDays = 3,
        settlingInDays = 1,
        pairCount = 2,
        checkInTime = LocalTime.of(21, 0),
        checkInDay = CheckInDay.SAME_DAY,
    ),
    schedule = if (started) {
        Schedule(
            startDate = LocalDate.of(2026, 3, 2),
            pairOrders = listOf(PairOrder.BASELINE_FIRST, PairOrder.INTERVENTION_FIRST),
            seed = 42,
        )
    } else {
        null
    },
    createdAt = CREATED,
    updatedAt = UPDATED,
)

fun checkIn(
    id: String = "check-in",
    experimentId: String = "experiment",
    studyDay: LocalDate = LocalDate.of(2026, 3, 2),
    answers: Map<QuestionId, Answer> = mapOf(
        sleepQuestion(experimentId).id to Answer.Rating(6),
        napQuestion(experimentId).id to Answer.YesNo(true),
        remarksQuestion(experimentId).id to Answer.FreeText("Late dinner"),
    ),
) = CheckIn(
    id = CheckInId(id),
    experimentId = ExperimentId(experimentId),
    studyDay = studyDay,
    followedPlan = true,
    answers = answers,
    recordedAt = CREATED,
    zone = ZoneId.of("America/New_York"),
    updatedAt = UPDATED,
)

fun note(id: String, parent: NoteParent) = Note(NoteId(id), parent, "Late coffee", CREATED, UPDATED)
