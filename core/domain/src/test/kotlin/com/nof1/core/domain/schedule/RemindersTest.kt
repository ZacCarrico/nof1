package com.nof1.core.domain.schedule

import com.nof1.core.domain.AB
import com.nof1.core.domain.BA
import com.nof1.core.domain.NEW_YORK
import com.nof1.core.domain.at
import com.nof1.core.domain.date
import com.nof1.core.domain.design
import com.nof1.core.domain.schedule
import com.nof1.core.model.CheckInDay
import java.time.Duration
import java.time.Instant
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RemindersTest {
    // Four 1-day periods around the switch to daylight saving time in New York (2026-03-08, 2am).
    private val design = design(
        periodLengthDays = 1,
        settlingInDays = 0,
        pairCount = 2,
        checkInTime = LocalTime.of(9, 0),
    )
    private val springForward = ExperimentTimeline(design, schedule(date(3, 7), AB, BA))

    @Test
    fun `daily reminders keep their local time across daylight saving changes`() {
        val reminders = springForward.studyDays.map { springForward.reminderTime(it, NEW_YORK) }

        assertEquals(Instant.parse("2026-03-07T14:00:00Z"), reminders[0])
        assertEquals(Instant.parse("2026-03-08T13:00:00Z"), reminders[1])
        assertEquals(Duration.ofHours(23), Duration.between(reminders[0], reminders[1]))
        reminders.forEach { assertEquals(LocalTime.of(9, 0), it.atZone(NEW_YORK).toLocalTime()) }
    }

    @Test
    fun `a reminder in the skipped hour moves forward by the gap`() {
        val timeline = ExperimentTimeline(
            design.copy(checkInTime = LocalTime.of(2, 30)),
            schedule(date(3, 7), AB, BA),
        )

        val reminder = timeline.reminderTime(date(3, 8), NEW_YORK)

        assertEquals(Instant.parse("2026-03-08T07:30:00Z"), reminder)
        assertEquals(LocalTime.of(3, 30), reminder.atZone(NEW_YORK).toLocalTime())
    }

    @Test
    fun `a reminder in the repeated hour uses its first occurrence`() {
        // Clocks go back from 2am to 1am on 2026-11-01, so 1:30 happens twice.
        val timeline = ExperimentTimeline(
            design(
                periodLengthDays = 1,
                settlingInDays = 0,
                pairCount = 1,
                checkInTime = LocalTime.of(1, 30),
            ),
            schedule(date(10, 31), AB),
        )

        assertEquals(
            Instant.parse("2026-11-01T05:30:00Z"),
            timeline.reminderTime(date(11, 1), NEW_YORK),
        )
    }

    @Test
    fun `the next reminder skips days that are already checked in`() {
        val after = date(3, 7).at(10)

        assertEquals(
            CheckInReminder(date(3, 8), Instant.parse("2026-03-08T13:00:00Z")),
            springForward.nextCheckInReminder(after, NEW_YORK, checkedInDays = emptySet()),
        )
        assertEquals(
            date(3, 9),
            springForward.nextCheckInReminder(
                after,
                NEW_YORK,
                checkedInDays = setOf(date(3, 8)),
            )?.studyDay,
        )
    }

    @Test
    fun `previous-day check-ins are reminded on the next day`() {
        val timeline = ExperimentTimeline(
            design.copy(checkInDay = CheckInDay.PREVIOUS_DAY),
            schedule(date(3, 7), AB, BA),
        )

        assertEquals(
            CheckInReminder(date(3, 7), Instant.parse("2026-03-08T13:00:00Z")),
            timeline.nextCheckInReminder(date(3, 7).at(10), NEW_YORK, checkedInDays = emptySet()),
        )
    }

    @Test
    fun `there are no reminders after the last study day`() {
        assertNull(springForward.nextCheckInReminder(date(3, 10).at(10), NEW_YORK, emptySet()))
    }

    @Test
    fun `period start notices announce each period on its first day`() {
        val timeline =
            ExperimentTimeline(
                design(periodLengthDays = 3, pairCount = 2),
                schedule(date(11, 2), AB, BA),
            )
        val noticeTime = LocalTime.of(8, 0)

        val first = timeline.nextPeriodStartNotice(date(11, 2).at(7), NEW_YORK, noticeTime)
        val second = timeline.nextPeriodStartNotice(date(11, 2).at(9), NEW_YORK, noticeTime)

        assertEquals(0, first?.period?.index)
        assertEquals(date(11, 2).at(8), first?.at)
        assertEquals(1, second?.period?.index)
        assertEquals(date(11, 5).at(8), second?.at)
    }

    @Test
    fun `no period start notices after an early stop`() {
        val stopped = ExperimentTimeline(
            design(periodLengthDays = 3, pairCount = 2),
            schedule(date(11, 2), AB, BA),
            stoppedOn = date(11, 7),
        )

        assertNull(stopped.nextPeriodStartNotice(date(11, 5).at(9), NEW_YORK, LocalTime.of(8, 0)))
    }
}
