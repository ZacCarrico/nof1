package com.nof1.core.domain.analysis

import com.nof1.core.domain.AB
import com.nof1.core.domain.BA
import com.nof1.core.domain.NEW_YORK
import com.nof1.core.domain.SLEEP
import com.nof1.core.domain.at
import com.nof1.core.domain.checkIn
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.domain.experiment
import com.nof1.core.domain.schedule
import com.nof1.core.domain.schedule.ExperimentStatus
import com.nof1.core.domain.schedule.ExperimentTimeline
import com.nof1.core.model.Answer
import com.nof1.core.model.AnswerType
import com.nof1.core.model.BetterDirection
import com.nof1.core.model.CheckIn
import com.nof1.core.model.Condition
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ExperimentResultsTest {
    private val energy = Question(QuestionId("energy"), "Energy today?", AnswerType.Rating())
    private val journal = Question(QuestionId("journal"), "Anything to note?", AnswerType.FreeText)

    // Five pairs of 3-day periods from 2026-11-02 to 12-01, with 1 settling-in day per period.
    private val design = design(periodLengthDays = 3, settlingInDays = 1, pairCount = 5)
    private val schedule = schedule(date(11, 2), AB, BA, BA, AB, AB)
    private val timeline = ExperimentTimeline(design, schedule)

    private fun started(
        betterDirection: BetterDirection = BetterDirection.HIGHER,
        stoppedOn: LocalDate? = null,
        asksAdherence: Boolean = true,
    ) = experiment(
        design = design,
        schedule = schedule,
        stoppedOn = stoppedOn,
        betterDirection = betterDirection,
        secondaryQuestions = listOf(energy, journal),
        asksAdherence = asksAdherence,
    )

    /**
     * A check-in every day. Analyzed days rate sleep 5 then 6 in baseline periods and 7 then 8 in
     * intervention periods (averages 5.5 and 7.5); settling-in days rate it one lower. The plan is
     * skipped on the first day of every intervention period.
     */
    private fun checkIns(through: LocalDate = timeline.lastDay): List<CheckIn> =
        timeline.studyDays.filter { it <= through }.map { day ->
            val period = requireNotNull(timeline.periodOn(day))
            val dayInPeriod = ChronoUnit.DAYS.between(period.firstDay, day).toInt()
            val base = if (period.condition == Condition.BASELINE) 5 else 7
            val note = if (day ==
                date(11, 3)
            ) {
                mapOf(journal.id to Answer.FreeText("Late coffee"))
            } else {
                emptyMap()
            }
            checkIn(
                studyDay = day,
                answers = mapOf(
                    SLEEP.id to Answer.Rating(base + dayInPeriod - 1),
                    energy.id to Answer.Rating(3),
                ) + note,
                followedPlan = !(period.condition == Condition.INTERVENTION && dayInPeriod == 0),
            )
        }

    @Test
    fun `a finished experiment gets a verdict`() {
        val results =
            computeResults(started(), checkIns(), now = date(12, 1).at(22), zone = NEW_YORK)

        assertEquals(ExperimentStatus.FINISHED, results.status)
        assertEquals(date(12, 1), results.asOf)
        assertEquals(5.5, results.primary.outcome.baselineAverage)
        assertEquals(7.5, results.primary.outcome.interventionAverage)
        assertEquals(List(5) { 2.0 }, results.primary.improvements)
        assertEquals(5, results.primary.pairsFavoringIntervention)
        assertEquals(1.0 / 32, results.primary.test?.pValueImprovement)
        assertEquals(VerdictResult(Verdict.HELPED), results.primary.verdict)
    }

    @Test
    fun `when lower is better, the same data means it made things worse`() {
        val results = computeResults(
            started(betterDirection = BetterDirection.LOWER),
            checkIns(),
            now = date(12, 1).at(22),
            zone = NEW_YORK,
        )

        assertEquals(List(5) { -2.0 }, results.primary.improvements)
        assertEquals(0, results.primary.pairsFavoringIntervention)
        assertEquals(VerdictResult(Verdict.MADE_IT_WORSE), results.primary.verdict)
    }

    @Test
    fun `there's no test or verdict while the experiment runs`() {
        val results =
            computeResults(
                started(),
                checkIns(through = date(11, 10)),
                date(11, 10).at(22),
                NEW_YORK,
            )

        assertEquals(ExperimentStatus.RUNNING, results.status)
        // Check-ins for 11-08 closed at the start of 11-10; later days can still be filled in.
        assertEquals(date(11, 8), results.asOf)
        assertNull(results.primary.test)
        assertNull(results.primary.verdict)
        assertEquals(1, results.primary.outcome.completePairs.size)
        assertEquals(PeriodStatus.NOT_OVER, results.primary.outcome.periods[2].status)
    }

    @Test
    fun `stopping early keeps complete pairs only`() {
        // The sixth period ends on 11-19, so three pairs are complete.
        val results =
            computeResults(
                started(stoppedOn = date(11, 19)),
                checkIns(),
                date(11, 19).at(22),
                NEW_YORK,
            )

        assertEquals(ExperimentStatus.STOPPED_EARLY, results.status)
        assertEquals(date(11, 19), results.asOf)
        assertEquals(3, results.primary.test?.pairCount)
        assertEquals(
            VerdictResult(Verdict.INCONCLUSIVE, InconclusiveReason.TOO_FEW_PAIRS),
            results.primary.verdict,
        )
        assertEquals(
            List(4) { PeriodStatus.NOT_OVER },
            results.primary.outcome.periods.drop(6).map { it.status },
        )
    }

    @Test
    fun `adherence is counted per condition`() {
        val results = computeResults(started(), checkIns(), date(12, 1).at(22), NEW_YORK)

        assertEquals(
            listOf(
                AdherenceResult(Condition.BASELINE, daysAnswered = 15, daysFollowed = 15),
                AdherenceResult(Condition.INTERVENTION, daysAnswered = 15, daysFollowed = 10),
            ),
            results.adherence,
        )
        assertEquals(
            emptyList(),
            computeResults(
                started(asksAdherence = false),
                checkIns(),
                date(12, 1).at(22),
                NEW_YORK,
            ).adherence,
        )
    }

    @Test
    fun `secondary questions are summarized without a verdict`() {
        val results = computeResults(started(), checkIns(), date(12, 1).at(22), NEW_YORK)

        val (energyResult, journalResult) = results.secondary
        energyResult as SecondaryOutcomeResult.Quantitative
        assertEquals(3.0, energyResult.outcome.baselineAverage)
        assertEquals(3.0, energyResult.outcome.interventionAverage)
        assertEquals(
            SecondaryOutcomeResult.Text(
                journal,
                listOf(TextAnswer(date(11, 3), Condition.BASELINE, "Late coffee")),
            ),
            journalResult,
        )
    }

    @Test
    fun `the most recently updated check-in for a day counts`() {
        val correction = checkIn(
            studyDay = date(11, 3),
            answers = mapOf(SLEEP.id to Answer.Rating(9)),
            updatedAt = Instant.EPOCH.plusSeconds(60),
        )
        val otherExperiment = correction.copy(experimentId = ExperimentId("other"))

        val results = computeResults(
            started(),
            checkIns() + correction + otherExperiment,
            date(12, 1).at(22),
            NEW_YORK,
        )

        // 11-03 now rates 9 instead of 5; 11-04 still rates 6.
        assertEquals(7.5, results.primary.outcome.periods.first().average)
    }

    @Test
    fun `results need a started experiment`() {
        assertFailsWith<IllegalArgumentException> {
            computeResults(experiment(design), emptyList(), date(11, 2).at(12), NEW_YORK)
        }
    }
}
