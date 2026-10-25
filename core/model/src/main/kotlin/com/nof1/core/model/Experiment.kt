package com.nof1.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * A structured N-of-1 experiment: alternating baseline (A) and intervention (B) periods in
 * randomized pairs, with a check-in every day.
 *
 * The [design] can be edited while the experiment is a draft. Starting it sets [schedule], after
 * which the design, conditions and primary outcome must not change.
 */
data class Experiment(
    val id: ExperimentId,
    val hypothesisId: HypothesisId,
    val title: String,
    val baseline: ConditionDescription,
    val intervention: ConditionDescription,
    val primaryOutcome: PrimaryOutcome,
    val secondaryQuestions: List<Question> = emptyList(),
    /** Whether each check-in also asks "Did you follow today's plan?". */
    val asksAdherence: Boolean = true,
    val design: ExperimentDesign,
    /** Set when the experiment starts; null while it's a draft. */
    val schedule: Schedule? = null,
    /** The last study day included if the experiment was stopped early. */
    val stoppedOn: LocalDate? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null,
) {
    init {
        if (schedule != null) {
            require(schedule.pairOrders.size == design.pairCount) {
                "Schedule has ${schedule.pairOrders.size} pairs, design has ${design.pairCount}"
            }
        }
        if (stoppedOn != null) {
            requireNotNull(schedule) { "Only a started experiment can be stopped" }
            val plannedLastDay = schedule.startDate.plusDays(design.totalDays - 1L)
            require(stoppedOn in schedule.startDate..plannedLastDay) {
                "stoppedOn ($stoppedOn) must fall between ${schedule.startDate} and $plannedLastDay"
            }
        }
        val questionIds = questions.map { it.id }
        require(questionIds.toSet().size == questionIds.size) { "Question IDs must be unique" }
    }

    /** All questions asked at each check-in, primary outcome first. */
    val questions: List<Question> get() = listOf(primaryOutcome.question) + secondaryQuestions

    fun describe(condition: Condition): ConditionDescription = when (condition) {
        Condition.BASELINE -> baseline
        Condition.INTERVENTION -> intervention
    }
}

/** The two conditions an experiment compares. */
enum class Condition {
    BASELINE,
    INTERVENTION,
}

/** What the user does during periods of one condition. */
data class ConditionDescription(val name: String, val instructions: String = "")

/** Which way the primary outcome should move for the intervention to count as an improvement. */
enum class BetterDirection {
    HIGHER,
    LOWER,
}

/**
 * The outcome that decides the verdict, and the smallest change the user said would matter to them.
 *
 * [minimumMeaningfulChange] is in the question's own units: rating points, the number's unit,
 * or for yes/no questions a share of days between 0 and 1 (0.2 means 20 percentage points).
 */
data class PrimaryOutcome(
    val question: Question,
    val betterDirection: BetterDirection,
    val minimumMeaningfulChange: Double,
) {
    init {
        require(question.type.isQuantitative) {
            "The primary outcome needs a rating, yes/no or number answer, not free text"
        }
        require(minimumMeaningfulChange.isFinite() && minimumMeaningfulChange > 0.0) {
            "minimumMeaningfulChange must be positive"
        }
        if (question.type == AnswerType.YesNo) {
            require(minimumMeaningfulChange <= 1.0) {
                "A yes/no outcome's minimumMeaningfulChange is a share of days, at most 1"
            }
        }
    }
}

/** Which day a check-in describes. */
enum class CheckInDay {
    /** The check-in is about the day it's done on, e.g. "How was your energy today?". */
    SAME_DAY,

    /** The check-in is about the day before, e.g. a morning check-in about last night's sleep. */
    PREVIOUS_DAY,
}

/** How an experiment runs. Fixed once the experiment starts. */
data class ExperimentDesign(
    val periodLengthDays: Int,
    /**
     * Days at the start of each period that follow the period's plan but aren't analyzed, so
     * effects carried over from the previous period can fade.
     */
    val settlingInDays: Int,
    /** Each pair has one baseline and one intervention period, in random order. */
    val pairCount: Int,
    val checkInTime: LocalTime,
    val checkInDay: CheckInDay,
) {
    init {
        require(periodLengthDays in PERIOD_LENGTH_DAYS) {
            "periodLengthDays must be in $PERIOD_LENGTH_DAYS, was $periodLengthDays"
        }
        require(settlingInDays in 0 until periodLengthDays) {
            "settlingInDays must leave at least one analyzed day per period, was $settlingInDays"
        }
        require(pairCount in PAIR_COUNT) { "pairCount must be in $PAIR_COUNT, was $pairCount" }
    }

    val totalDays: Int get() = 2 * pairCount * periodLengthDays

    val analyzedDaysPerPeriod: Int get() = periodLengthDays - settlingInDays

    companion object {
        val PERIOD_LENGTH_DAYS = 1..28
        val PAIR_COUNT = 1..30
    }
}

/** Which condition comes first within a pair. */
enum class PairOrder {
    BASELINE_FIRST,
    INTERVENTION_FIRST,
}

/**
 * When a started experiment began and the random order of its pairs. The [seed] that produced
 * [pairOrders] is kept so the order can be checked later.
 */
data class Schedule(val startDate: LocalDate, val pairOrders: List<PairOrder>, val seed: Long) {
    init {
        require(pairOrders.isNotEmpty()) { "A schedule needs at least one pair" }
    }
}
