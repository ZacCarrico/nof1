package com.nof1.core.domain.schedule

import com.nof1.core.model.CheckInDay
import com.nof1.core.model.Condition
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentDesign
import com.nof1.core.model.PairOrder
import com.nof1.core.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** A block of consecutive days under one condition. */
data class Period(
    /** Position in the experiment, starting at 0. */
    val index: Int,
    val condition: Condition,
    val firstDay: LocalDate,
    val lastDay: LocalDate,
    /** The first day after the settling-in days. */
    val firstAnalyzedDay: LocalDate,
) {
    val pairIndex: Int get() = index / 2

    operator fun contains(day: LocalDate): Boolean = day in firstDay..lastDay

    fun isSettlingIn(day: LocalDate): Boolean = day in this && day < firstAnalyzedDay
}

/** When the check-in for one study day can be entered; [closesAt] is exclusive. */
data class CheckInWindow(val opensAt: Instant, val closesAt: Instant) {
    operator fun contains(instant: Instant): Boolean = instant >= opensAt && instant < closesAt
}

/**
 * The calendar of a started experiment: which condition applies on each day, and when each day's
 * check-in is due.
 *
 * A study day is a day the plan applies to. Its check-in is due that day, or the next day when
 * check-ins describe the previous day. It can be entered from the start of the due date until the
 * end of the day after, so a missed day can still be filled in the next day.
 */
class ExperimentTimeline(
    val design: ExperimentDesign,
    val schedule: Schedule,
    stoppedOn: LocalDate? = null,
) {
    init {
        require(schedule.pairOrders.size == design.pairCount) {
            "Schedule has ${schedule.pairOrders.size} pairs, design has ${design.pairCount}"
        }
        require(stoppedOn == null || stoppedOn >= schedule.startDate) {
            "stoppedOn ($stoppedOn) is before the start date (${schedule.startDate})"
        }
    }

    val firstDay: LocalDate = schedule.startDate

    val plannedLastDay: LocalDate = firstDay.plusDays(design.totalDays - 1L)

    /** The planned last day, or the stop day if the experiment was stopped early. */
    val lastDay: LocalDate =
        if (stoppedOn != null && stoppedOn < plannedLastDay) stoppedOn else plannedLastDay

    /** Every planned period, including any after an early stop. */
    val periods: List<Period> = schedule.pairOrders.flatMapIndexed { pairIndex, order ->
        val conditions = when (order) {
            PairOrder.BASELINE_FIRST -> listOf(Condition.BASELINE, Condition.INTERVENTION)
            PairOrder.INTERVENTION_FIRST -> listOf(Condition.INTERVENTION, Condition.BASELINE)
        }
        conditions.mapIndexed { offset, condition ->
            val index = 2 * pairIndex + offset
            val periodStart = firstDay.plusDays(index.toLong() * design.periodLengthDays)
            Period(
                index = index,
                condition = condition,
                firstDay = periodStart,
                lastDay = periodStart.plusDays(design.periodLengthDays - 1L),
                firstAnalyzedDay = periodStart.plusDays(design.settlingInDays.toLong()),
            )
        }
    }

    val studyDays: List<LocalDate> =
        generateSequence(firstDay) { it.plusDays(1) }.takeWhile { it <= lastDay }.toList()

    fun isStudyDay(day: LocalDate): Boolean = day in firstDay..lastDay

    /** The planned period a day falls in, or null outside the planned days. */
    fun periodOn(day: LocalDate): Period? {
        if (day < firstDay || day > plannedLastDay) return null
        val offset = ChronoUnit.DAYS.between(firstDay, day)
        return periods[(offset / design.periodLengthDays).toInt()]
    }

    /** Whether a day's check-in counts toward results: a study day that isn't a settling-in day. */
    fun isAnalyzed(day: LocalDate): Boolean =
        isStudyDay(day) && periodOn(day)?.isSettlingIn(day) == false

    fun dueDate(studyDay: LocalDate): LocalDate = when (design.checkInDay) {
        CheckInDay.SAME_DAY -> studyDay
        CheckInDay.PREVIOUS_DAY -> studyDay.plusDays(1)
    }

    /** The study day whose check-in is due on [date], if there is one. */
    fun studyDayDueOn(date: LocalDate): LocalDate? =
        studyDayFor(dueDate = date).takeIf(::isStudyDay)

    fun checkInWindow(studyDay: LocalDate, zone: ZoneId): CheckInWindow {
        val due = dueDate(studyDay)
        return CheckInWindow(
            opensAt = due.atStartOfDay(zone).toInstant(),
            closesAt = due.plusDays(2).atStartOfDay(zone).toInstant(),
        )
    }

    /**
     * When the reminder for a study day's check-in goes off: the check-in time on its due date, in
     * local time. Times skipped by a daylight-saving change move forward by the length of the gap.
     */
    fun reminderTime(studyDay: LocalDate, zone: ZoneId): Instant =
        dueDate(studyDay).atTime(design.checkInTime).atZone(zone).toInstant()

    /** Study days whose check-in can be entered at [now], oldest first. */
    fun openStudyDays(now: Instant, zone: ZoneId): List<LocalDate> {
        val today = now.atZone(zone).toLocalDate()
        return listOf(today.minusDays(1), today)
            .mapNotNull(::studyDayDueOn)
            .filter { now in checkInWindow(it, zone) }
    }

    /**
     * The latest study day whose check-in is due on or before [date], or null if none is. This is
     * the last day included when the experiment is stopped on [date].
     */
    fun lastStudyDayDueBy(date: LocalDate): LocalDate? {
        val studyDay = studyDayFor(dueDate = date)
        return when {
            studyDay < firstDay -> null
            studyDay > lastDay -> lastDay
            else -> studyDay
        }
    }

    /** Whether no more check-ins can arrive: each study day is checked in or past its window. */
    fun isFinished(now: Instant, zone: ZoneId, checkedInDays: Set<LocalDate>): Boolean =
        studyDays.asReversed().all {
            it in checkedInDays || checkInWindow(it, zone).closesAt <= now
        }

    private fun studyDayFor(dueDate: LocalDate): LocalDate = when (design.checkInDay) {
        CheckInDay.SAME_DAY -> dueDate
        CheckInDay.PREVIOUS_DAY -> dueDate.minusDays(1)
    }
}

/** The timeline of a started experiment, or null for a draft. */
fun Experiment.timeline(): ExperimentTimeline? =
    schedule?.let { ExperimentTimeline(design, it, stoppedOn) }
