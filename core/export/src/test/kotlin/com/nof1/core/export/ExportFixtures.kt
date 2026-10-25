package com.nof1.core.export

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

internal val CREATED: Instant = Instant.parse("2026-03-01T18:30:00Z")
internal val UPDATED: Instant = Instant.parse("2026-03-01T18:45:10.250Z")
internal val NEW_YORK: ZoneId = ZoneId.of("America/New_York")

internal val PROJECT_ID = ProjectId("project-1")
internal val HYPOTHESIS_ID = HypothesisId("hypothesis-1")
internal val EXPERIMENT_ID = ExperimentId("experiment-1")

internal val SLEEP = Question(
    QuestionId("q-sleep"),
    "How rested do you feel?",
    AnswerType.Rating(1, 10, "Exhausted", "Fully rested"),
)
internal val CAFFEINE = Question(
    QuestionId("q-caffeine"),
    "How much caffeine did you have?",
    AnswerType.NumberWithUnit("mg", decimals = 0, min = 0.0, max = 1000.0),
)
internal val NAP = Question(QuestionId("q-nap"), "Did you nap?", AnswerType.YesNo)
internal val REMARKS = Question(QuestionId("q-remarks"), "Anything unusual?", AnswerType.FreeText)

internal fun project() = Project(PROJECT_ID, "Sleep better", "Wake up rested", CREATED, UPDATED)

internal fun hypothesis() = Hypothesis(
    id = HYPOTHESIS_ID,
    projectId = PROJECT_ID,
    statement = "Screens late at night make my sleep worse",
    details = "",
    createdAt = CREATED,
    updatedAt = UPDATED,
)

/**
 * Periods of 3 days with 1 settling-in day, starting 2026-03-02:
 * baseline 2–4, intervention 5–7, intervention 8–10, baseline 11–13 (all March).
 */
internal fun experiment(title: String = "No screens after 10pm") = Experiment(
    id = EXPERIMENT_ID,
    hypothesisId = HYPOTHESIS_ID,
    title = title,
    baseline = ConditionDescription("Usual evenings"),
    intervention = ConditionDescription("No screens", "Phone in the kitchen from 10pm"),
    primaryOutcome = PrimaryOutcome(SLEEP, BetterDirection.HIGHER, 1.5),
    secondaryQuestions = listOf(CAFFEINE, NAP, REMARKS),
    design = ExperimentDesign(
        periodLengthDays = 3,
        settlingInDays = 1,
        pairCount = 2,
        checkInTime = LocalTime.of(21, 0),
        checkInDay = CheckInDay.SAME_DAY,
    ),
    schedule = Schedule(
        startDate = LocalDate.of(2026, 3, 2),
        pairOrders = listOf(PairOrder.BASELINE_FIRST, PairOrder.INTERVENTION_FIRST),
        seed = 7_042_116_955_323_408_291,
    ),
    createdAt = CREATED,
    updatedAt = UPDATED,
)

internal fun draftExperiment() = Experiment(
    id = ExperimentId("experiment-2"),
    hypothesisId = HYPOTHESIS_ID,
    title = "Morning walk",
    baseline = ConditionDescription("No walk"),
    intervention = ConditionDescription("Walk", "20 minutes outside before 9am"),
    primaryOutcome = PrimaryOutcome(
        Question(QuestionId("q-energy"), "Energy today", AnswerType.Rating()),
        BetterDirection.HIGHER,
        1.0,
    ),
    asksAdherence = false,
    design = ExperimentDesign(7, 2, 5, LocalTime.of(20, 30), CheckInDay.SAME_DAY),
    createdAt = CREATED,
    updatedAt = UPDATED,
    archivedAt = UPDATED,
)

internal fun checkIn(
    day: Int,
    answers: Map<QuestionId, Answer> = mapOf(
        // Not in question order, to check that exports order them.
        REMARKS.id to Answer.FreeText("Late dinner"),
        SLEEP.id to Answer.Rating(6),
        NAP.id to Answer.YesNo(false),
        CAFFEINE.id to Answer.NumberWithUnit(95.0),
    ),
    followedPlan: Boolean? = true,
) = CheckIn(
    id = CheckInId("check-in-$day"),
    experimentId = EXPERIMENT_ID,
    studyDay = LocalDate.of(2026, 3, day),
    followedPlan = followedPlan,
    answers = answers,
    recordedAt = LocalDate.of(2026, 3, day).atTime(21, 5).atZone(NEW_YORK).toInstant(),
    zone = NEW_YORK,
    updatedAt = LocalDate.of(2026, 3, day).atTime(21, 5).atZone(NEW_YORK).toInstant(),
)

internal fun note() = Note(
    id = NoteId("note-1"),
    parent = NoteParent.OfExperiment(EXPERIMENT_ID),
    text = "Had a \"big\" coffee at 4pm,\nslept badly",
    createdAt = CREATED,
    updatedAt = UPDATED,
)

internal fun exportData(
    experiments: List<Experiment> = listOf(experiment(), draftExperiment()),
    checkIns: List<CheckIn> = listOf(checkIn(2)),
) = ExportData(
    projects = listOf(project()),
    hypotheses = listOf(hypothesis()),
    experiments = experiments,
    checkIns = checkIns,
    notes = listOf(note()),
)
