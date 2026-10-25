package com.nof1.core.domain.analysis

import com.nof1.core.domain.AB
import com.nof1.core.domain.BA
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.domain.schedule
import com.nof1.core.domain.schedule.ExperimentTimeline
import com.nof1.core.model.Answer
import com.nof1.core.model.Condition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OutcomeAnalysisTest {
    // Periods: baseline 11-02..04, intervention 11-05..07, intervention 11-08..10 and
    // baseline 11-11..13. The first day of each period is a settling-in day.
    private val design = design(periodLengthDays = 3, settlingInDays = 1, pairCount = 2)
    private val schedule = schedule(date(11, 2), AB, BA)
    private val timeline = ExperimentTimeline(design, schedule)

    private val answers = mapOf(
        date(11, 2) to 1.0, // settling-in, ignored
        date(11, 3) to 4.0,
        date(11, 4) to 6.0,
        date(11, 5) to 10.0, // settling-in, ignored
        date(11, 6) to 7.0, // 11-07 missing
        date(11, 9) to 8.0,
        date(11, 10) to 8.0,
        date(11, 12) to 5.0,
        date(11, 13) to 7.0,
    )

    @Test
    fun `only analyzed days count, and half of them must be answered`() {
        val result = analyzeOutcome(timeline, answers, asOf = date(11, 13))

        assertEquals(listOf(5.0, 7.0, 8.0, 6.0), result.periods.map { it.average })
        assertEquals(listOf(2, 1, 2, 2), result.periods.map { it.daysWithAnswers })
        assertEquals(List(4) { PeriodStatus.COMPLETE }, result.periods.map { it.status })
        assertEquals(2, result.periods.first().analyzedDays)

        val missingLastPeriod = answers - date(11, 12) - date(11, 13)
        val partial = analyzeOutcome(timeline, missingLastPeriod, asOf = date(11, 13))
        assertEquals(PeriodStatus.TOO_FEW_ANSWERS, partial.periods[3].status)
        assertNull(partial.periods[3].average)
        assertNull(partial.pairs[1].difference)
    }

    @Test
    fun `pairs subtract baseline from intervention whatever their order`() {
        val result = analyzeOutcome(timeline, answers, asOf = date(11, 13))

        assertEquals(listOf(2.0, 2.0), result.pairs.map { it.difference })
        assertEquals(
            listOf(Condition.BASELINE, Condition.BASELINE),
            result.pairs.map { it.baseline.period.condition },
        )
        assertEquals(5.5, result.baselineAverage)
        assertEquals(7.5, result.interventionAverage)
    }

    @Test
    fun `periods ending after the analysis date aren't over yet`() {
        val result = analyzeOutcome(timeline, answers, asOf = date(11, 7))

        assertEquals(
            listOf(
                PeriodStatus.COMPLETE,
                PeriodStatus.COMPLETE,
                PeriodStatus.NOT_OVER,
                PeriodStatus.NOT_OVER,
            ),
            result.periods.map { it.status },
        )
        assertEquals(8.0, result.periods[2].average)
        assertEquals(1, result.completePairs.size)
        assertEquals(
            List(4) { PeriodStatus.NOT_OVER },
            analyzeOutcome(timeline, answers, asOf = null).periods.map { it.status },
        )
    }

    @Test
    fun `periods cut short by an early stop don't count`() {
        val stopped = ExperimentTimeline(design, schedule, stoppedOn = date(11, 8))

        val result = analyzeOutcome(stopped, answers, asOf = date(11, 8))

        assertEquals(PeriodStatus.NOT_OVER, result.periods[2].status)
        assertEquals(0, result.periods[2].daysWithAnswers)
    }

    @Test
    fun `yes-no answers average to the share of yes days`() {
        val result =
            analyzeOutcome(
                timeline,
                mapOf(date(11, 3) to 1.0, date(11, 4) to 0.0),
                asOf = date(11, 4),
            )

        assertEquals(0.5, result.periods.first().average)
        assertEquals(1.0, Answer.YesNo(true).numericValue())
        assertEquals(0.0, Answer.YesNo(false).numericValue())
        assertEquals(7.0, Answer.Rating(7).numericValue())
        assertEquals(7.5, Answer.NumberWithUnit(7.5).numericValue())
        assertNull(Answer.FreeText("Slept badly").numericValue())
    }
}
