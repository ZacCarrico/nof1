package com.nof1.core.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The JSON export file, version 1. Instants are ISO-8601 in UTC ("2026-03-02T21:05:00Z"), dates
// and times of day are ISO-8601 ("2026-03-02", "21:00"), and enums are their constant names.
// Answer types and answers are objects whose "type" says which kind they are.

@Serializable
internal data class ExportFile(
    val format: String,
    val version: Int,
    val exportedAt: String,
    val projects: List<ProjectJson>,
    val hypotheses: List<HypothesisJson>,
    val experiments: List<ExperimentJson>,
    val checkIns: List<CheckInJson>,
    val notes: List<NoteJson>,
)

@Serializable
internal data class ProjectJson(
    val id: String,
    val name: String,
    val goal: String,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String?,
)

@Serializable
internal data class HypothesisJson(
    val id: String,
    val projectId: String,
    val statement: String,
    val details: String,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String?,
)

@Serializable
internal data class ExperimentJson(
    val id: String,
    val hypothesisId: String,
    val title: String,
    val baseline: ConditionJson,
    val intervention: ConditionJson,
    val primaryOutcome: PrimaryOutcomeJson,
    val secondaryQuestions: List<QuestionJson>,
    val asksAdherence: Boolean,
    val design: DesignJson,
    /** Null while the experiment is a draft. */
    val schedule: ScheduleJson?,
    val stoppedOn: String?,
    val createdAt: String,
    val updatedAt: String,
    val archivedAt: String?,
)

@Serializable
internal data class ConditionJson(val name: String, val instructions: String)

@Serializable
internal data class PrimaryOutcomeJson(
    val question: QuestionJson,
    val betterDirection: String,
    val minimumMeaningfulChange: Double,
)

@Serializable
internal data class QuestionJson(val id: String, val prompt: String, val answerType: AnswerTypeJson)

@Serializable
internal sealed interface AnswerTypeJson {
    @Serializable
    @SerialName("rating")
    data class Rating(val min: Int, val max: Int, val minLabel: String, val maxLabel: String) :
        AnswerTypeJson

    @Serializable
    @SerialName("yesNo")
    data object YesNo : AnswerTypeJson

    @Serializable
    @SerialName("number")
    data class Number(val unit: String, val decimals: Int, val min: Double?, val max: Double?) :
        AnswerTypeJson

    @Serializable
    @SerialName("freeText")
    data object FreeText : AnswerTypeJson
}

@Serializable
internal data class DesignJson(
    val periodLengthDays: Int,
    val settlingInDays: Int,
    val pairCount: Int,
    val checkInTime: String,
    val checkInDay: String,
)

@Serializable
internal data class ScheduleJson(
    val startDate: String,
    val pairOrders: List<String>,
    val seed: Long,
)

@Serializable
internal data class CheckInJson(
    val id: String,
    val experimentId: String,
    val studyDay: String,
    val followedPlan: Boolean?,
    val answers: List<AnswerJson>,
    val recordedAt: String,
    val timeZone: String,
    val updatedAt: String,
)

@Serializable
internal sealed interface AnswerJson {
    val questionId: String

    @Serializable
    @SerialName("rating")
    data class Rating(override val questionId: String, val value: Int) : AnswerJson

    @Serializable
    @SerialName("yesNo")
    data class YesNo(override val questionId: String, val value: Boolean) : AnswerJson

    @Serializable
    @SerialName("number")
    data class Number(override val questionId: String, val value: Double) : AnswerJson

    @Serializable
    @SerialName("freeText")
    data class FreeText(override val questionId: String, val text: String) : AnswerJson
}

@Serializable
internal data class NoteJson(
    val id: String,
    /** "project", "hypothesis" or "experiment". */
    val parentType: String,
    val parentId: String,
    val text: String,
    val createdAt: String,
    val updatedAt: String,
)
