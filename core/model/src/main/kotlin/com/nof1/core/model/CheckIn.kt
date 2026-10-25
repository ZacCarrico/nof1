package com.nof1.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The answers for one study day of an experiment.
 *
 * [studyDay] is the day the answers describe. For experiments whose check-ins describe the previous
 * day, it's the day before the check-in was done.
 */
data class CheckIn(
    val id: CheckInId,
    val experimentId: ExperimentId,
    val studyDay: LocalDate,
    /** The answer to "Did you follow today's plan?", or null if not asked or skipped. */
    val followedPlan: Boolean?,
    val answers: Map<QuestionId, Answer>,
    val recordedAt: Instant,
    /** The time zone the check-in was recorded in. */
    val zone: ZoneId,
    val updatedAt: Instant,
)
