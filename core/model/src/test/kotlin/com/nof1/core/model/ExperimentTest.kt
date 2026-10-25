package com.nof1.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExperimentTest {
    private val sleepQuality = Question(
        id = QuestionId("sleep"),
        prompt = "Rate last night's sleep",
        type = AnswerType.Rating(min = 1, max = 10),
    )

    private val design = ExperimentDesign(
        periodLengthDays = 3,
        settlingInDays = 1,
        pairCount = 5,
        checkInTime = LocalTime.of(9, 0),
        checkInDay = CheckInDay.PREVIOUS_DAY,
    )

    private fun experiment(
        schedule: Schedule? = null,
        stoppedOn: LocalDate? = null,
        secondaryQuestions: List<Question> = emptyList(),
    ) = Experiment(
        id = ExperimentId("experiment"),
        hypothesisId = HypothesisId("hypothesis"),
        title = "No screens after 10pm",
        baseline = ConditionDescription("Usual routine"),
        intervention = ConditionDescription("No screens", "No screens after 10pm"),
        primaryOutcome = PrimaryOutcome(sleepQuality, BetterDirection.HIGHER, 1.0),
        secondaryQuestions = secondaryQuestions,
        design = design,
        schedule = schedule,
        stoppedOn = stoppedOn,
        createdAt = Instant.EPOCH,
        updatedAt = Instant.EPOCH,
    )

    private fun schedule(pairs: Int = 5) = Schedule(
        startDate = LocalDate.of(2026, 11, 2),
        pairOrders = List(pairs) { PairOrder.BASELINE_FIRST },
        seed = 1,
    )

    @Test
    fun `primary outcome must be quantitative`() {
        val journal = Question(QuestionId("journal"), "Anything to note?", AnswerType.FreeText)

        assertFailsWith<IllegalArgumentException> {
            PrimaryOutcome(journal, BetterDirection.HIGHER, 1.0)
        }
    }

    @Test
    fun `minimum meaningful change must be positive and at most 1 for yes-no outcomes`() {
        assertFailsWith<IllegalArgumentException> {
            PrimaryOutcome(sleepQuality, BetterDirection.HIGHER, 0.0)
        }
        val headache = Question(QuestionId("headache"), "Headache today?", AnswerType.YesNo)
        assertFailsWith<IllegalArgumentException> {
            PrimaryOutcome(headache, BetterDirection.LOWER, 1.5)
        }
        PrimaryOutcome(headache, BetterDirection.LOWER, 0.2)
    }

    @Test
    fun `design needs at least one analyzed day per period and limits its size`() {
        assertFailsWith<IllegalArgumentException> { design.copy(settlingInDays = 3) }
        assertFailsWith<IllegalArgumentException> { design.copy(periodLengthDays = 0) }
        assertFailsWith<IllegalArgumentException> { design.copy(pairCount = 31) }
    }

    @Test
    fun `design counts total and analyzed days`() {
        assertEquals(30, design.totalDays)
        assertEquals(2, design.analyzedDaysPerPeriod)
    }

    @Test
    fun `schedule must match the design's pair count`() {
        assertFailsWith<IllegalArgumentException> { experiment(schedule = schedule(pairs = 4)) }
    }

    @Test
    fun `only a started experiment can stop, and only within its days`() {
        assertFailsWith<IllegalArgumentException> {
            experiment(stoppedOn = LocalDate.of(2026, 11, 5))
        }
        assertFailsWith<IllegalArgumentException> {
            experiment(schedule = schedule(), stoppedOn = LocalDate.of(2026, 11, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            experiment(schedule = schedule(), stoppedOn = LocalDate.of(2026, 12, 2))
        }
        experiment(schedule = schedule(), stoppedOn = LocalDate.of(2026, 12, 1))
    }

    @Test
    fun `question IDs must be unique and the primary outcome comes first`() {
        val duplicate = sleepQuality.copy(prompt = "Same ID")
        assertFailsWith<IllegalArgumentException> {
            experiment(secondaryQuestions = listOf(duplicate))
        }

        val energy = Question(QuestionId("energy"), "Energy today?", AnswerType.Rating())
        val questions = experiment(secondaryQuestions = listOf(energy)).questions
        assertEquals(listOf(sleepQuality, energy), questions)
    }
}
