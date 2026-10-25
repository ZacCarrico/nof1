package com.nof1.core.domain.schedule

import com.nof1.core.domain.AB
import com.nof1.core.domain.BA
import com.nof1.core.domain.NEW_YORK
import com.nof1.core.domain.at
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.domain.schedule
import com.nof1.core.model.CheckInDay
import com.nof1.core.model.Condition
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExperimentTimelineTest {
    // Two pairs of 3-day periods with 1 settling-in day, starting Monday 2026-11-02.
    private val design = design(periodLengthDays = 3, settlingInDays = 1, pairCount = 2)
    private val schedule = schedule(date(11, 2), AB, BA)
    private val timeline = ExperimentTimeline(design, schedule)

    @Test
    fun `periods follow the pair orders`() {
        assertEquals(
            listOf(
                Period(0, Condition.BASELINE, date(11, 2), date(11, 4), date(11, 3)),
                Period(1, Condition.INTERVENTION, date(11, 5), date(11, 7), date(11, 6)),
                Period(2, Condition.INTERVENTION, date(11, 8), date(11, 10), date(11, 9)),
                Period(3, Condition.BASELINE, date(11, 11), date(11, 13), date(11, 12)),
            ),
            timeline.periods,
        )
        assertEquals(listOf(0, 0, 1, 1), timeline.periods.map { it.pairIndex })
        assertEquals(date(11, 13), timeline.lastDay)
        assertEquals(12, timeline.studyDays.size)
    }

    @Test
    fun `finds the period of each planned day`() {
        assertEquals(1, timeline.periodOn(date(11, 7))?.index)
        assertEquals(3, timeline.periodOn(date(11, 13))?.index)
        assertNull(timeline.periodOn(date(11, 1)))
        assertNull(timeline.periodOn(date(11, 14)))
    }

    @Test
    fun `settling-in days and days outside the experiment aren't analyzed`() {
        assertFalse(timeline.isAnalyzed(date(11, 2)))
        assertTrue(timeline.isAnalyzed(date(11, 3)))
        assertFalse(timeline.isAnalyzed(date(11, 5)))
        assertTrue(timeline.isAnalyzed(date(11, 6)))
        assertFalse(timeline.isAnalyzed(date(11, 14)))
    }

    @Test
    fun `same-day check-ins are due on the study day`() {
        assertEquals(date(11, 3), timeline.dueDate(date(11, 3)))
        assertEquals(date(11, 3), timeline.studyDayDueOn(date(11, 3)))
        assertNull(timeline.studyDayDueOn(date(11, 14)))
    }

    @Test
    fun `previous-day check-ins are due the next day`() {
        val timeline =
            ExperimentTimeline(design.copy(checkInDay = CheckInDay.PREVIOUS_DAY), schedule)

        assertEquals(date(11, 4), timeline.dueDate(date(11, 3)))
        assertNull(timeline.studyDayDueOn(date(11, 2)))
        assertEquals(date(11, 13), timeline.studyDayDueOn(date(11, 14)))
    }

    @Test
    fun `a check-in can be entered from its due date until the end of the next day`() {
        val window = timeline.checkInWindow(date(11, 3), NEW_YORK)

        assertEquals(Instant.parse("2026-11-03T05:00:00Z"), window.opensAt)
        assertEquals(Instant.parse("2026-11-05T05:00:00Z"), window.closesAt)
        assertTrue(date(11, 4).at(23, 59) in window)
        assertFalse(date(11, 5).at(0, 0) in window)
    }

    @Test
    fun `open check-ins include yesterday's until it closes`() {
        assertEquals(listOf(date(11, 2)), timeline.openStudyDays(date(11, 2).at(8), NEW_YORK))
        assertEquals(
            listOf(date(11, 3), date(11, 4)),
            timeline.openStudyDays(date(11, 4).at(10), NEW_YORK),
        )
        assertEquals(listOf(date(11, 13)), timeline.openStudyDays(date(11, 14).at(10), NEW_YORK))
        assertEquals(emptyList(), timeline.openStudyDays(date(11, 15).at(10), NEW_YORK))
    }

    @Test
    fun `stopping early ends the study days but keeps the planned periods`() {
        val stopped = ExperimentTimeline(design, schedule, stoppedOn = date(11, 7))

        assertEquals(date(11, 7), stopped.lastDay)
        assertEquals(date(11, 13), stopped.plannedLastDay)
        assertEquals(6, stopped.studyDays.size)
        assertFalse(stopped.isStudyDay(date(11, 8)))
        assertEquals(4, stopped.periods.size)
    }

    @Test
    fun `the stop day is the last study day already due`() {
        assertEquals(date(11, 5), timeline.lastStudyDayDueBy(date(11, 5)))
        assertNull(timeline.lastStudyDayDueBy(date(11, 1)))
        assertEquals(date(11, 13), timeline.lastStudyDayDueBy(date(11, 20)))

        val previousDay =
            ExperimentTimeline(design.copy(checkInDay = CheckInDay.PREVIOUS_DAY), schedule)
        assertEquals(date(11, 4), previousDay.lastStudyDayDueBy(date(11, 5)))
        assertNull(previousDay.lastStudyDayDueBy(date(11, 2)))
    }
}
