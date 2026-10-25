package com.nof1.core.domain.schedule

import com.nof1.core.model.Experiment
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class ExperimentStatus {
    /** Being designed; can still be edited. */
    DRAFT,

    /** Started with a future start date. The plan is locked. */
    SCHEDULED,

    RUNNING,

    /** Every study day is checked in or past its check-in window, so results are final. */
    FINISHED,

    STOPPED_EARLY,
}

fun Experiment.status(now: Instant, zone: ZoneId, checkedInDays: Set<LocalDate>): ExperimentStatus {
    val timeline = timeline() ?: return ExperimentStatus.DRAFT
    return when {
        stoppedOn != null -> ExperimentStatus.STOPPED_EARLY
        now.atZone(zone).toLocalDate() < timeline.firstDay -> ExperimentStatus.SCHEDULED
        timeline.isFinished(now, zone, checkedInDays) -> ExperimentStatus.FINISHED
        else -> ExperimentStatus.RUNNING
    }
}
