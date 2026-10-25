package com.nof1.core.database.model

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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MappersTest {
    @Test
    fun projectRoundTrips() {
        val project = Project(PROJECT_ID, "Sleep better", "Fall asleep faster", CREATED, UPDATED)

        assertEquals(project, project.asEntity().asExternalModel())
        assertEquals(
            project.copy(archivedAt = ARCHIVED),
            project.copy(archivedAt = ARCHIVED).asEntity().asExternalModel(),
        )
    }

    @Test
    fun timestampsAreStoredAsEpochMilliseconds() {
        val entity = Project(PROJECT_ID, "Sleep better", "", CREATED, UPDATED, ARCHIVED).asEntity()

        assertEquals(1_772_400_000_123, entity.createdAt)
        assertEquals(1_772_486_400_456, entity.updatedAt)
        assertEquals(1_772_572_800_789, entity.archivedAt)
    }

    @Test
    fun timestampsLoseSubMillisecondPrecision() {
        val project = Project(PROJECT_ID, "", "", Instant.ofEpochSecond(10, 123_456_789), UPDATED)

        assertEquals(Instant.ofEpochMilli(10_123), project.asEntity().asExternalModel().createdAt)
    }

    @Test
    fun hypothesisRoundTrips() {
        val hypothesis = Hypothesis(
            id = HypothesisId("hypothesis"),
            projectId = PROJECT_ID,
            statement = "Screens late at night delay my sleep",
            details = "Especially the phone",
            createdAt = CREATED,
            updatedAt = UPDATED,
            archivedAt = ARCHIVED,
        )

        assertEquals(hypothesis, hypothesis.asEntity().asExternalModel())
    }

    @Test
    fun startedExperimentWithEveryAnswerTypeRoundTrips() {
        val experiment = startedExperiment()
        // Room returns related rows in no particular order.
        val populated =
            PopulatedExperiment(experiment.asEntity(), experiment.questionEntities().reversed())

        assertEquals(experiment, populated.asExternalModel())
    }

    @Test
    fun draftExperimentRoundTrips() {
        val draft = startedExperiment().copy(schedule = null, stoppedOn = null, archivedAt = null)
        val entity = draft.asEntity()

        assertNull(entity.startDate)
        assertNull(entity.pairOrders)
        assertNull(entity.scheduleSeed)
        assertEquals(draft, PopulatedExperiment(entity, draft.questionEntities()).asExternalModel())
    }

    @Test
    fun experimentStoresDatesTimesAndEnumsAsText() {
        val entity = startedExperiment().asEntity()

        assertEquals("2026-03-02", entity.startDate)
        assertEquals("2026-03-20", entity.stoppedOn)
        assertEquals("21:30", entity.checkInTime)
        assertEquals("PREVIOUS_DAY", entity.checkInDay)
        assertEquals("LOWER", entity.betterDirection)
        assertEquals(
            "BASELINE_FIRST,INTERVENTION_FIRST,INTERVENTION_FIRST,BASELINE_FIRST",
            entity.pairOrders,
        )
        assertEquals(-8_123_456_789, entity.scheduleSeed)
    }

    @Test
    fun questionsAreStoredPrimaryFirstWithOnlyTheirTypesSettings() {
        val questions = startedExperiment().questionEntities()

        assertEquals(listOf(0, 1, 2, 3, 4), questions.map { it.position })
        assertEquals(
            listOf("RATING", "NUMBER", "YES_NO", "FREE_TEXT", "NUMBER"),
            questions.map { it.answerType },
        )
        val rating = questions[0]
        assertEquals(listOf(1, 10), listOf(rating.ratingMin, rating.ratingMax))
        assertEquals(
            listOf("Badly", "Very well"),
            listOf(rating.ratingMinLabel, rating.ratingMaxLabel),
        )
        assertNull(rating.numberUnit)
        val yesNo = questions[2]
        assertEquals(
            listOf(null, null, null, null),
            listOf(yesNo.ratingMin, yesNo.numberUnit, yesNo.numberDecimals, yesNo.numberMin),
        )
    }

    @Test
    fun checkInWithEveryAnswerTypeRoundTrips() {
        val checkIn = checkIn()
        val populated = PopulatedCheckIn(checkIn.asEntity(), checkIn.answerEntities())

        assertEquals(checkIn, populated.asExternalModel())
    }

    @Test
    fun checkInWithoutAdherenceAnswerRoundTrips() {
        val checkIn = checkIn().copy(followedPlan = null, answers = emptyMap())
        val populated = PopulatedCheckIn(checkIn.asEntity(), checkIn.answerEntities())

        assertEquals(checkIn, populated.asExternalModel())
    }

    @Test
    fun answersSetOnlyTheirOwnValueColumn() {
        val answers = checkIn().answerEntities().associateBy { it.questionId }

        assertEquals(
            AnswerEntity(CHECK_IN_ID.value, SLEEP.id.value, "RATING", 7, null, null, null),
            answers.getValue(SLEEP.id.value),
        )
        assertEquals(
            AnswerEntity(CHECK_IN_ID.value, NAP.id.value, "YES_NO", null, false, null, null),
            answers.getValue(NAP.id.value),
        )
        assertEquals(
            AnswerEntity(CHECK_IN_ID.value, HOURS.id.value, "NUMBER", null, null, 7.5, null),
            answers.getValue(HOURS.id.value),
        )
        assertEquals(
            AnswerEntity(
                CHECK_IN_ID.value,
                REMARKS.id.value,
                "FREE_TEXT",
                null,
                null,
                null,
                "Late dinner",
            ),
            answers.getValue(REMARKS.id.value),
        )
    }

    @Test
    fun checkInStoresDayAndTimeZoneAsText() {
        val entity = checkIn().asEntity()

        assertEquals("2026-03-05", entity.studyDay)
        assertEquals("America/New_York", entity.timeZone)
    }

    @Test
    fun notesRoundTripForEveryParent() {
        val parents = listOf(
            NoteParent.OfProject(PROJECT_ID),
            NoteParent.OfHypothesis(HypothesisId("hypothesis")),
            NoteParent.OfExperiment(EXPERIMENT_ID),
        )
        for (parent in parents) {
            val note = Note(NoteId("note"), parent, "Late coffee today", CREATED, UPDATED)
            assertEquals(note, note.asEntity().asExternalModel())
        }
    }

    @Test
    fun noteSetsOnlyItsParentsColumn() {
        val entity =
            Note(NoteId("note"), NoteParent.OfExperiment(EXPERIMENT_ID), "", CREATED, UPDATED)
                .asEntity()

        assertEquals(
            listOf(null, null, EXPERIMENT_ID.value),
            listOf(entity.projectId, entity.hypothesisId, entity.experimentId),
        )
    }

    @Test
    fun unknownStoredValuesFail() {
        val question = SLEEP.asEntity(EXPERIMENT_ID, 0)
        assertFailsWith<IllegalStateException> {
            question.copy(answerType = "SLIDER").asExternalModel()
        }
        assertFailsWith<IllegalArgumentException> {
            question.copy(ratingMax = null).asExternalModel()
        }
        val answer = Answer.Rating(3).asEntity(CHECK_IN_ID, SLEEP.id)
        assertFailsWith<IllegalArgumentException> { answer.copy(rating = null).asExternalModel() }
        val note = Note(NoteId("note"), NoteParent.OfProject(PROJECT_ID), "", CREATED, UPDATED)
        assertFailsWith<IllegalStateException> {
            note.asEntity().copy(projectId = null).asExternalModel()
        }
    }

    private companion object {
        val PROJECT_ID = ProjectId("project")
        val EXPERIMENT_ID = ExperimentId("experiment")
        val CHECK_IN_ID = CheckInId("check-in")
        val CREATED: Instant = Instant.ofEpochMilli(1_772_400_000_123)
        val UPDATED: Instant = Instant.ofEpochMilli(1_772_486_400_456)
        val ARCHIVED: Instant = Instant.ofEpochMilli(1_772_572_800_789)

        val SLEEP = Question(
            QuestionId("sleep"),
            "How rested do you feel?",
            AnswerType.Rating(1, 10, "Badly", "Very well"),
        )
        val CAFFEINE = Question(
            QuestionId("caffeine"),
            "How much caffeine did you have?",
            AnswerType.NumberWithUnit("mg", decimals = 0, min = 0.0, max = 1000.0),
        )
        val NAP = Question(QuestionId("nap"), "Did you nap?", AnswerType.YesNo)
        val REMARKS = Question(QuestionId("remarks"), "Anything unusual?", AnswerType.FreeText)
        val HOURS = Question(QuestionId("hours"), "Hours slept", AnswerType.NumberWithUnit("h", 1))

        fun startedExperiment() = Experiment(
            id = EXPERIMENT_ID,
            hypothesisId = HypothesisId("hypothesis"),
            title = "No screens after 10pm",
            baseline = ConditionDescription("Usual evenings"),
            intervention = ConditionDescription("No screens", "Phone in the kitchen from 10pm"),
            primaryOutcome = PrimaryOutcome(SLEEP, BetterDirection.LOWER, 1.5),
            secondaryQuestions = listOf(CAFFEINE, NAP, REMARKS, HOURS),
            asksAdherence = false,
            design = ExperimentDesign(
                periodLengthDays = 3,
                settlingInDays = 1,
                pairCount = 4,
                checkInTime = LocalTime.of(21, 30),
                checkInDay = CheckInDay.PREVIOUS_DAY,
            ),
            schedule = Schedule(
                startDate = LocalDate.of(2026, 3, 2),
                pairOrders = listOf(
                    PairOrder.BASELINE_FIRST,
                    PairOrder.INTERVENTION_FIRST,
                    PairOrder.INTERVENTION_FIRST,
                    PairOrder.BASELINE_FIRST,
                ),
                seed = -8_123_456_789,
            ),
            stoppedOn = LocalDate.of(2026, 3, 20),
            createdAt = CREATED,
            updatedAt = UPDATED,
            archivedAt = ARCHIVED,
        )

        fun checkIn() = CheckIn(
            id = CHECK_IN_ID,
            experimentId = EXPERIMENT_ID,
            studyDay = LocalDate.of(2026, 3, 5),
            followedPlan = true,
            answers = mapOf(
                SLEEP.id to Answer.Rating(7),
                NAP.id to Answer.YesNo(false),
                HOURS.id to Answer.NumberWithUnit(7.5),
                REMARKS.id to Answer.FreeText("Late dinner"),
            ),
            recordedAt = CREATED,
            zone = ZoneId.of("America/New_York"),
            updatedAt = UPDATED,
        )
    }
}
