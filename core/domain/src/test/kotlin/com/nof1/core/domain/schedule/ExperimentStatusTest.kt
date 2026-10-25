package com.nof1.core.domain.schedule

import com.nof1.core.domain.AB
import com.nof1.core.domain.NEW_YORK
import com.nof1.core.domain.at
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.domain.experiment
import com.nof1.core.domain.schedule
import kotlin.test.Test
import kotlin.test.assertEquals

class ExperimentStatusTest {
    // One pair of 1-day periods: study days 2026-11-02 and 11-03, checked in at 9pm the same day.
    private val design = design(periodLengthDays = 1, settlingInDays = 0, pairCount = 1)
    private val started = experiment(design, schedule(date(11, 2), AB))

    @Test
    fun `an experiment without a schedule is a draft`() {
        assertEquals(
            ExperimentStatus.DRAFT,
            experiment(design).status(date(11, 2).at(12), NEW_YORK, emptySet()),
        )
    }

    @Test
    fun `a started experiment is scheduled until its first day`() {
        assertEquals(
            ExperimentStatus.SCHEDULED,
            started.status(date(11, 1).at(23), NEW_YORK, emptySet()),
        )
        assertEquals(
            ExperimentStatus.RUNNING,
            started.status(date(11, 2).at(0), NEW_YORK, emptySet()),
        )
    }

    @Test
    fun `it finishes once the last check-in window closes, even with missing days`() {
        // The 11-03 check-in can be entered until the end of 11-04.
        assertEquals(
            ExperimentStatus.RUNNING,
            started.status(date(11, 4).at(23, 59), NEW_YORK, emptySet()),
        )
        assertEquals(
            ExperimentStatus.FINISHED,
            started.status(date(11, 5).at(0), NEW_YORK, emptySet()),
        )
    }

    @Test
    fun `it finishes as soon as every day is checked in`() {
        val allDays = setOf(date(11, 2), date(11, 3))

        assertEquals(
            ExperimentStatus.FINISHED,
            started.status(date(11, 3).at(22), NEW_YORK, allDays),
        )
    }

    @Test
    fun `a stopped experiment is stopped early`() {
        val stopped = experiment(design, schedule(date(11, 2), AB), stoppedOn = date(11, 2))

        assertEquals(
            ExperimentStatus.STOPPED_EARLY,
            stopped.status(date(11, 2).at(22), NEW_YORK, emptySet()),
        )
    }
}
