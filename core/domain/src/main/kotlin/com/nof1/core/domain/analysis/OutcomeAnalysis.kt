package com.nof1.core.domain.analysis

import com.nof1.core.domain.schedule.ExperimentTimeline
import com.nof1.core.domain.schedule.Period
import com.nof1.core.model.Answer
import com.nof1.core.model.Condition
import java.time.LocalDate

/** Whether a period's answers count toward results. */
enum class PeriodStatus {
    /** Over, with answers on at least half of its analyzed days. */
    COMPLETE,

    /** Over, but answered on fewer than half of its analyzed days. */
    TOO_FEW_ANSWERS,

    /** Still running, not started yet, or cut short by an early stop. */
    NOT_OVER,
}

data class PeriodResult(
    val period: Period,
    /** The period's length minus its settling-in days. */
    val analyzedDays: Int,
    val daysWithAnswers: Int,
    /** The average answer (the share of "yes" days for yes/no questions); null without answers. */
    val average: Double?,
    val status: PeriodStatus,
)

data class PairResult(
    val pairIndex: Int,
    val baseline: PeriodResult,
    val intervention: PeriodResult,
    /** Intervention average minus baseline average, when both periods are complete. */
    val difference: Double?,
)

/** Descriptive results for one quantitative question. */
data class OutcomeResult(
    val periods: List<PeriodResult>,
    val pairs: List<PairResult>,
    /** The average of the baseline periods in complete pairs; null without complete pairs. */
    val baselineAverage: Double?,
    /** The average of the intervention periods in complete pairs; null without complete pairs. */
    val interventionAverage: Double?,
) {
    val completePairs: List<PairResult> get() = pairs.filter { it.difference != null }
}

/**
 * Period and pair results for one quantitative question.
 *
 * [answers] holds each study day's answer as a number (see [numericValue]); only analyzed days
 * count. Periods ending after [asOf], the last study day whose data is final, are
 * [PeriodStatus.NOT_OVER].
 */
fun analyzeOutcome(
    timeline: ExperimentTimeline,
    answers: Map<LocalDate, Double>,
    asOf: LocalDate?,
): OutcomeResult {
    val analyzedDays = timeline.design.analyzedDaysPerPeriod
    val periods = timeline.periods.map { period ->
        val values = generateSequence(period.firstAnalyzedDay) { it.plusDays(1) }
            .takeWhile { it <= period.lastDay && timeline.isStudyDay(it) }
            .mapNotNull { answers[it] }
            .toList()
        val status = when {
            asOf == null || period.lastDay > asOf || period.lastDay > timeline.lastDay ->
                PeriodStatus.NOT_OVER

            2 * values.size < analyzedDays -> PeriodStatus.TOO_FEW_ANSWERS

            else -> PeriodStatus.COMPLETE
        }
        PeriodResult(period, analyzedDays, values.size, values.averageOrNull(), status)
    }
    val pairs = periods.chunked(2).mapIndexed { pairIndex, (first, second) ->
        val baselineFirst = first.period.condition == Condition.BASELINE
        val baseline = if (baselineFirst) first else second
        val intervention = if (baselineFirst) second else first
        PairResult(pairIndex, baseline, intervention, difference(baseline, intervention))
    }
    val complete = pairs.filter { it.difference != null }
    return OutcomeResult(
        periods = periods,
        pairs = pairs,
        baselineAverage = complete.mapNotNull { it.baseline.average }.averageOrNull(),
        interventionAverage = complete.mapNotNull { it.intervention.average }.averageOrNull(),
    )
}

/** The answer as a number for averaging: the rating, the measured amount, or 1/0 for yes/no. */
fun Answer.numericValue(): Double? = when (this) {
    is Answer.Rating -> value.toDouble()
    is Answer.YesNo -> if (value) 1.0 else 0.0
    is Answer.NumberWithUnit -> value
    is Answer.FreeText -> null
}

private fun difference(baseline: PeriodResult, intervention: PeriodResult): Double? {
    val baselineAverage = baseline.average
    val interventionAverage = intervention.average
    val bothComplete = baseline.status == PeriodStatus.COMPLETE &&
        intervention.status == PeriodStatus.COMPLETE
    return if (bothComplete && baselineAverage != null && interventionAverage != null) {
        interventionAverage - baselineAverage
    } else {
        null
    }
}

private fun List<Double>.averageOrNull(): Double? = if (isEmpty()) null else average()
