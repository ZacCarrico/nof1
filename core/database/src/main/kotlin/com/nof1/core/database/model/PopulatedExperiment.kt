package com.nof1.core.database.model

import androidx.room3.Embedded
import androidx.room3.Relation
import com.nof1.core.model.BetterDirection
import com.nof1.core.model.CheckInDay
import com.nof1.core.model.ConditionDescription
import com.nof1.core.model.Experiment
import com.nof1.core.model.ExperimentDesign
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.PairOrder
import com.nof1.core.model.PrimaryOutcome
import com.nof1.core.model.Schedule
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** An experiment with its questions. */
data class PopulatedExperiment(
    @Embedded val experiment: ExperimentEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["experiment_id"])
    val questions: List<QuestionEntity>,
)

fun PopulatedExperiment.asExternalModel(): Experiment {
    val orderedQuestions = questions.sortedBy { it.position }.map { it.asExternalModel() }
    require(orderedQuestions.isNotEmpty()) { "Experiment ${experiment.id} has no questions" }
    return with(experiment) {
        Experiment(
            id = ExperimentId(id),
            hypothesisId = HypothesisId(hypothesisId),
            title = title,
            baseline = ConditionDescription(baselineName, baselineInstructions),
            intervention = ConditionDescription(interventionName, interventionInstructions),
            primaryOutcome = PrimaryOutcome(
                question = orderedQuestions.first(),
                betterDirection = BetterDirection.valueOf(betterDirection),
                minimumMeaningfulChange = minimumMeaningfulChange,
            ),
            secondaryQuestions = orderedQuestions.drop(1),
            asksAdherence = asksAdherence,
            design = ExperimentDesign(
                periodLengthDays = periodLengthDays,
                settlingInDays = settlingInDays,
                pairCount = pairCount,
                checkInTime = LocalTime.parse(checkInTime),
                checkInDay = CheckInDay.valueOf(checkInDay),
            ),
            schedule = schedule(),
            stoppedOn = stoppedOn?.let(LocalDate::parse),
            createdAt = Instant.ofEpochMilli(createdAt),
            updatedAt = Instant.ofEpochMilli(updatedAt),
            archivedAt = archivedAt?.let(Instant::ofEpochMilli),
        )
    }
}

private fun ExperimentEntity.schedule(): Schedule? {
    if (startDate == null) return null
    return Schedule(
        startDate = LocalDate.parse(startDate),
        pairOrders = requireNotNull(pairOrders) { "Started experiment $id has no pair orders" }
            .split(PAIR_ORDER_SEPARATOR)
            .map(PairOrder::valueOf),
        seed = requireNotNull(scheduleSeed) { "Started experiment $id has no seed" },
    )
}

fun Experiment.asEntity(): ExperimentEntity = ExperimentEntity(
    id = id.value,
    hypothesisId = hypothesisId.value,
    title = title,
    baselineName = baseline.name,
    baselineInstructions = baseline.instructions,
    interventionName = intervention.name,
    interventionInstructions = intervention.instructions,
    betterDirection = primaryOutcome.betterDirection.name,
    minimumMeaningfulChange = primaryOutcome.minimumMeaningfulChange,
    asksAdherence = asksAdherence,
    periodLengthDays = design.periodLengthDays,
    settlingInDays = design.settlingInDays,
    pairCount = design.pairCount,
    checkInTime = design.checkInTime.toString(),
    checkInDay = design.checkInDay.name,
    startDate = schedule?.startDate?.toString(),
    pairOrders = schedule?.pairOrders?.joinToString(PAIR_ORDER_SEPARATOR) { it.name },
    scheduleSeed = schedule?.seed,
    stoppedOn = stoppedOn?.toString(),
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    archivedAt = archivedAt?.toEpochMilli(),
)

/** The experiment's questions, primary outcome first. */
fun Experiment.questionEntities(): List<QuestionEntity> =
    questions.mapIndexed { position, question -> question.asEntity(id, position) }

private const val PAIR_ORDER_SEPARATOR = ","
