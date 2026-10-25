package com.nof1.core.domain.analysis

import com.nof1.core.domain.schedule.ExperimentStatus
import com.nof1.core.domain.schedule.status
import com.nof1.core.domain.schedule.timeline
import com.nof1.core.model.Answer
import com.nof1.core.model.BetterDirection
import com.nof1.core.model.CheckIn
import com.nof1.core.model.Condition
import com.nof1.core.model.Experiment
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class ExperimentResults(
    val status: ExperimentStatus,
    /** The last study day whose data is final, or null if none is yet. */
    val asOf: LocalDate?,
    val primary: PrimaryOutcomeResult,
    /** Empty when the experiment doesn't ask about adherence. */
    val adherence: List<AdherenceResult>,
    val secondary: List<SecondaryOutcomeResult>,
)

data class PrimaryOutcomeResult(
    val outcome: OutcomeResult,
    val betterDirection: BetterDirection,
    /** Each complete pair's difference, with the sign flipped when lower is better. */
    val improvements: List<Double>,
    /** Computed only once the experiment has finished or stopped. */
    val test: RandomizationTestResult?,
    /** Decided only once the experiment has finished or stopped. */
    val verdict: VerdictResult?,
) {
    val pairsFavoringIntervention: Int get() = improvements.count { it > 0.0 }
}

/** How often the plan was followed, over all check-ins in one condition's periods. */
data class AdherenceResult(
    val condition: Condition,
    val daysAnswered: Int,
    val daysFollowed: Int,
) {
    val share: Double? get() = if (daysAnswered ==
        0
    ) {
        null
    } else {
        daysFollowed.toDouble() / daysAnswered
    }
}

/** Results for a secondary question. These are descriptive only and never affect the verdict. */
sealed interface SecondaryOutcomeResult {
    val question: Question

    data class Quantitative(override val question: Question, val outcome: OutcomeResult) :
        SecondaryOutcomeResult

    data class Text(override val question: Question, val answers: List<TextAnswer>) :
        SecondaryOutcomeResult
}

data class TextAnswer(val studyDay: LocalDate, val condition: Condition, val text: String)

/**
 * Results for a started experiment as of [now]. The randomization test and verdict are only
 * computed once the experiment has finished or been stopped, so looking at results while it runs
 * can't bias them.
 *
 * If a study day has several check-ins, the most recently updated one counts.
 */
fun computeResults(
    experiment: Experiment,
    checkIns: List<CheckIn>,
    now: Instant,
    zone: ZoneId,
): ExperimentResults {
    val timeline = requireNotNull(experiment.timeline()) { "Results need a started experiment" }
    val latestByDay = checkIns
        .filter { it.experimentId == experiment.id && timeline.isStudyDay(it.studyDay) }
        .groupBy { it.studyDay }
        .mapValues { (_, sameDay) -> sameDay.maxBy { it.updatedAt } }
    val status = experiment.status(now, zone, latestByDay.keys)
    val isFinal = status == ExperimentStatus.FINISHED || status == ExperimentStatus.STOPPED_EARLY
    val asOf = if (isFinal) {
        timeline.lastDay
    } else {
        timeline.studyDays.lastOrNull { timeline.checkInWindow(it, zone).closesAt <= now }
    }

    val primary = experiment.primaryOutcome
    val primaryOutcome =
        analyzeOutcome(timeline, latestByDay.numericAnswers(primary.question.id), asOf)
    val sign = if (primary.betterDirection == BetterDirection.HIGHER) 1.0 else -1.0
    val improvements = primaryOutcome.completePairs.mapNotNull { pair ->
        pair.difference?.let {
            it *
                sign
        }
    }
    val test = if (isFinal &&
        improvements.isNotEmpty()
    ) {
        RandomizationTest.run(improvements)
    } else {
        null
    }

    val adherence = if (experiment.asksAdherence) {
        Condition.entries.map { condition ->
            val answers = latestByDay.values
                .filter { timeline.periodOn(it.studyDay)?.condition == condition }
                .mapNotNull { it.followedPlan }
            AdherenceResult(
                condition,
                daysAnswered = answers.size,
                daysFollowed = answers.count {
                    it
                },
            )
        }
    } else {
        emptyList()
    }

    val secondary = experiment.secondaryQuestions.map { question ->
        if (question.type.isQuantitative) {
            SecondaryOutcomeResult.Quantitative(
                question,
                analyzeOutcome(timeline, latestByDay.numericAnswers(question.id), asOf),
            )
        } else {
            val answers = latestByDay.values.sortedBy { it.studyDay }.mapNotNull { checkIn ->
                val text = (checkIn.answers[question.id] as? Answer.FreeText)?.text
                val condition = timeline.periodOn(checkIn.studyDay)?.condition
                if (text != null &&
                    condition != null
                ) {
                    TextAnswer(checkIn.studyDay, condition, text)
                } else {
                    null
                }
            }
            SecondaryOutcomeResult.Text(question, answers)
        }
    }

    return ExperimentResults(
        status = status,
        asOf = asOf,
        primary = PrimaryOutcomeResult(
            outcome = primaryOutcome,
            betterDirection = primary.betterDirection,
            improvements = improvements,
            test = test,
            verdict = if (isFinal) Verdicts.decide(test, primary.minimumMeaningfulChange) else null,
        ),
        adherence = adherence,
        secondary = secondary,
    )
}

private fun Map<LocalDate, CheckIn>.numericAnswers(questionId: QuestionId): Map<LocalDate, Double> =
    mapNotNull { (day, checkIn) ->
        checkIn.answers[questionId]?.numericValue()?.let { day to it }
    }.toMap()
