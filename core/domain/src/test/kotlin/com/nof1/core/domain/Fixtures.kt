package com.nof1.core.domain

import com.nof1.core.model.Answer
import com.nof1.core.model.AnswerType
import com.nof1.core.model.BetterDirection
import com.nof1.core.model.CheckIn
import com.nof1.core.model.CheckInDay
import com.nof1.core.model.CheckInId
import com.nof1.core.model.ConditionDescription
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentDesign
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.PairOrder
import com.nof1.core.model.PrimaryOutcome
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId
import com.nof1.core.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

internal val NEW_YORK: ZoneId = ZoneId.of("America/New_York")

internal val AB = PairOrder.BASELINE_FIRST
internal val BA = PairOrder.INTERVENTION_FIRST

internal val EXPERIMENT_ID = ExperimentId("experiment")

internal val SLEEP =
    Question(QuestionId("sleep"), "Rate last night's sleep", AnswerType.Rating(1, 10))

internal fun design(
    periodLengthDays: Int = 3,
    settlingInDays: Int = 1,
    pairCount: Int = 5,
    checkInTime: LocalTime = LocalTime.of(21, 0),
    checkInDay: CheckInDay = CheckInDay.SAME_DAY,
) = ExperimentDesign(periodLengthDays, settlingInDays, pairCount, checkInTime, checkInDay)

internal fun schedule(startDate: LocalDate, vararg pairOrders: PairOrder) =
    Schedule(startDate, pairOrders.toList(), seed = 0)

internal fun experiment(
    design: ExperimentDesign,
    schedule: Schedule? = null,
    stoppedOn: LocalDate? = null,
    primaryQuestion: Question = SLEEP,
    betterDirection: BetterDirection = BetterDirection.HIGHER,
    minimumMeaningfulChange: Double = 1.0,
    secondaryQuestions: List<Question> = emptyList(),
    asksAdherence: Boolean = true,
) = Experiment(
    id = EXPERIMENT_ID,
    hypothesisId = HypothesisId("hypothesis"),
    title = "No screens after 10pm",
    baseline = ConditionDescription("Usual routine"),
    intervention = ConditionDescription("No screens", "No screens after 10pm"),
    primaryOutcome = PrimaryOutcome(primaryQuestion, betterDirection, minimumMeaningfulChange),
    secondaryQuestions = secondaryQuestions,
    asksAdherence = asksAdherence,
    design = design,
    schedule = schedule,
    stoppedOn = stoppedOn,
    createdAt = Instant.EPOCH,
    updatedAt = Instant.EPOCH,
)

internal fun checkIn(
    studyDay: LocalDate,
    answers: Map<QuestionId, Answer>,
    followedPlan: Boolean? = true,
    updatedAt: Instant = Instant.EPOCH,
) = CheckIn(
    id = CheckInId.random(),
    experimentId = EXPERIMENT_ID,
    studyDay = studyDay,
    followedPlan = followedPlan,
    answers = answers,
    recordedAt = updatedAt,
    zone = NEW_YORK,
    updatedAt = updatedAt,
)

/** The instant of a local time on this date in [zone]. */
internal fun LocalDate.at(hour: Int, minute: Int = 0, zone: ZoneId = NEW_YORK): Instant =
    atTime(hour, minute).atZone(zone).toInstant()

internal fun date(month: Int, day: Int, year: Int = 2026): LocalDate =
    LocalDate.of(year, month, day)
