package com.nof1.core.export

import com.nof1.core.model.Answer
import com.nof1.core.model.AnswerType
import com.nof1.core.model.CheckIn
import com.nof1.core.model.ConditionDescription
import com.nof1.core.model.Experiment
import com.nof1.core.model.Hypothesis
import com.nof1.core.model.Note
import com.nof1.core.model.NoteParent
import com.nof1.core.model.Project
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId
import java.time.Instant
import kotlinx.serialization.json.Json

/**
 * Writes everything the user has entered as one JSON document, in a versioned format that a future
 * import can read back.
 */
object JsonExport {
    const val FORMAT = "nof1-export"
    const val VERSION = 1

    private val json = Json { prettyPrint = true }

    fun encode(data: ExportData, exportedAt: Instant): String =
        json.encodeToString(ExportFile.serializer(), data.toExportFile(exportedAt))
}

private fun ExportData.toExportFile(exportedAt: Instant): ExportFile {
    val questionOrder = experiments.associate { experiment ->
        experiment.id to experiment.questions.map { it.id }
    }
    return ExportFile(
        format = JsonExport.FORMAT,
        version = JsonExport.VERSION,
        exportedAt = exportedAt.toString(),
        projects = projects.map { it.toJson() },
        hypotheses = hypotheses.map { it.toJson() },
        experiments = experiments.map { it.toJson() },
        checkIns = checkIns.map { it.toJson(questionOrder[it.experimentId].orEmpty()) },
        notes = notes.map { it.toJson() },
    )
}

private fun Project.toJson() = ProjectJson(
    id = id.value,
    name = name,
    goal = goal,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
)

private fun Hypothesis.toJson() = HypothesisJson(
    id = id.value,
    projectId = projectId.value,
    statement = statement,
    details = details,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
)

private fun Experiment.toJson() = ExperimentJson(
    id = id.value,
    hypothesisId = hypothesisId.value,
    title = title,
    baseline = baseline.toJson(),
    intervention = intervention.toJson(),
    primaryOutcome = PrimaryOutcomeJson(
        question = primaryOutcome.question.toJson(),
        betterDirection = primaryOutcome.betterDirection.name,
        minimumMeaningfulChange = primaryOutcome.minimumMeaningfulChange,
    ),
    secondaryQuestions = secondaryQuestions.map { it.toJson() },
    asksAdherence = asksAdherence,
    design = DesignJson(
        periodLengthDays = design.periodLengthDays,
        settlingInDays = design.settlingInDays,
        pairCount = design.pairCount,
        checkInTime = design.checkInTime.toString(),
        checkInDay = design.checkInDay.name,
    ),
    schedule = schedule?.let { schedule ->
        ScheduleJson(
            startDate = schedule.startDate.toString(),
            pairOrders = schedule.pairOrders.map { it.name },
            seed = schedule.seed,
        )
    },
    stoppedOn = stoppedOn?.toString(),
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
    archivedAt = archivedAt?.toString(),
)

private fun ConditionDescription.toJson() = ConditionJson(name, instructions)

private fun Question.toJson() = QuestionJson(
    id = id.value,
    prompt = prompt,
    answerType = when (val type = type) {
        is AnswerType.Rating -> AnswerTypeJson.Rating(
            type.min,
            type.max,
            type.minLabel,
            type.maxLabel,
        )

        AnswerType.YesNo -> AnswerTypeJson.YesNo

        is AnswerType.NumberWithUnit ->
            AnswerTypeJson.Number(type.unit, type.decimals, type.min, type.max)

        AnswerType.FreeText -> AnswerTypeJson.FreeText
    },
)

/** Answers are listed in the experiment's question order, primary outcome first. */
private fun CheckIn.toJson(questionOrder: List<QuestionId>) = CheckInJson(
    id = id.value,
    experimentId = experimentId.value,
    studyDay = studyDay.toString(),
    followedPlan = followedPlan,
    answers = answersInOrder(questionOrder).map { (questionId, answer) ->
        when (answer) {
            is Answer.Rating -> AnswerJson.Rating(questionId.value, answer.value)
            is Answer.YesNo -> AnswerJson.YesNo(questionId.value, answer.value)
            is Answer.NumberWithUnit -> AnswerJson.Number(questionId.value, answer.value)
            is Answer.FreeText -> AnswerJson.FreeText(questionId.value, answer.text)
        }
    },
    recordedAt = recordedAt.toString(),
    timeZone = zone.id,
    updatedAt = updatedAt.toString(),
)

private fun Note.toJson(): NoteJson {
    val (parentType, parentId) = when (val parent = parent) {
        is NoteParent.OfProject -> "project" to parent.projectId.value
        is NoteParent.OfHypothesis -> "hypothesis" to parent.hypothesisId.value
        is NoteParent.OfExperiment -> "experiment" to parent.experimentId.value
    }
    return NoteJson(
        id = id.value,
        parentType = parentType,
        parentId = parentId,
        text = text,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
