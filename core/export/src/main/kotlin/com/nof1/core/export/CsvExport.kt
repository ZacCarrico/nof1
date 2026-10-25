package com.nof1.core.export

import com.nof1.core.domain.schedule.timeline
import com.nof1.core.model.Answer
import com.nof1.core.model.AnswerType
import com.nof1.core.model.CheckIn
import com.nof1.core.model.Condition
import com.nof1.core.model.Experiment
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId

/**
 * Writes check-in answers as CSV for spreadsheets and statistics tools, in long format: one row per
 * answer, with the period, pair and condition of its day. A check-in without answers still gets a
 * row, for its adherence answer.
 *
 * Periods and pairs are numbered from 1. Days outside the study, such as after an early stop, have
 * no period or condition.
 */
object CsvExport {
    val COLUMNS = listOf(
        "experiment_id",
        "experiment_title",
        "study_day",
        "period",
        "pair",
        "condition",
        "condition_name",
        "settling_in",
        "followed_plan",
        "question_id",
        "question",
        "answer_type",
        "value",
        "unit",
        "recorded_at",
        "time_zone",
    )

    /** The question_id to unit columns of a row without an answer. */
    private val NO_ANSWER = List(5) { "" }

    fun checkIns(data: ExportData): String = buildString {
        append(Csv.line(COLUMNS))
        val experiments = data.experiments.associateBy { it.id }
        for (checkIn in data.checkIns) {
            val experiment = experiments[checkIn.experimentId] ?: continue
            for (row in rows(experiment, checkIn)) append(Csv.line(row))
        }
    }

    private fun rows(experiment: Experiment, checkIn: CheckIn): List<List<String>> {
        val day = checkIn.studyDay
        val period = experiment.timeline()?.takeIf { it.isStudyDay(day) }?.periodOn(day)
        val dayFields = listOf(
            experiment.id.value,
            Csv.userText(experiment.title),
            day.toString(),
            period?.let { (it.index + 1).toString() }.orEmpty(),
            period?.let { (it.pairIndex + 1).toString() }.orEmpty(),
            period?.condition?.code.orEmpty(),
            period?.let { Csv.userText(experiment.describe(it.condition).name) }.orEmpty(),
            period?.isSettlingIn(day)?.toString().orEmpty(),
            checkIn.followedPlan?.toString().orEmpty(),
        )
        val recordedFields = listOf(checkIn.recordedAt.toString(), checkIn.zone.id)

        val questions = experiment.questions.associateBy { it.id }
        val answers = checkIn.answersInOrder(experiment.questions.map { it.id })
        if (answers.isEmpty()) return listOf(dayFields + NO_ANSWER + recordedFields)
        return answers.map { (questionId, answer) ->
            dayFields + answerFields(questionId, questions[questionId], answer) + recordedFields
        }
    }

    private fun answerFields(questionId: QuestionId, question: Question?, answer: Answer) = listOf(
        questionId.value,
        Csv.userText(question?.prompt.orEmpty()),
        when (answer) {
            is Answer.Rating -> "rating"
            is Answer.YesNo -> "yes_no"
            is Answer.NumberWithUnit -> "number"
            is Answer.FreeText -> "free_text"
        },
        when (answer) {
            is Answer.Rating -> answer.value.toString()
            is Answer.YesNo -> answer.value.toString()
            is Answer.NumberWithUnit -> Csv.number(answer.value)
            is Answer.FreeText -> Csv.userText(answer.text)
        },
        Csv.userText((question?.type as? AnswerType.NumberWithUnit)?.unit.orEmpty()),
    )

    private val Condition.code: String
        get() = when (this) {
            Condition.BASELINE -> "baseline"
            Condition.INTERVENTION -> "intervention"
        }
}
