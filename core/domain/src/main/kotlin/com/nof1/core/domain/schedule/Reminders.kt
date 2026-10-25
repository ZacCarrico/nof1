package com.nof1.core.domain.schedule

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class CheckInReminder(val studyDay: LocalDate, val at: Instant)

data class PeriodStartNotice(val period: Period, val at: Instant)

/** The first check-in reminder after [after] for a study day that isn't checked in yet. */
fun ExperimentTimeline.nextCheckInReminder(
    after: Instant,
    zone: ZoneId,
    checkedInDays: Set<LocalDate>,
): CheckInReminder? = studyDays.asSequence()
    .filter { it !in checkedInDays }
    .map { CheckInReminder(it, reminderTime(it, zone)) }
    .firstOrNull { it.at > after }

/**
 * The first notice after [after] that a period is starting, sent at [noticeTime] on the period's
 * first day.
 */
fun ExperimentTimeline.nextPeriodStartNotice(
    after: Instant,
    zone: ZoneId,
    noticeTime: LocalTime,
): PeriodStartNotice? = periods.asSequence()
    .filter { isStudyDay(it.firstDay) }
    .map { PeriodStartNotice(it, it.firstDay.atTime(noticeTime).atZone(zone).toInstant()) }
    .firstOrNull { it.at > after }
