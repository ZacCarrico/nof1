package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.nof1.core.model.AnswerType
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.Question
import com.nof1.core.model.QuestionId

/**
 * A question asked at each check-in. The columns for an answer type's settings are null for the
 * other answer types.
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = ExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("experiment_id")],
)
data class QuestionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "experiment_id") val experimentId: String,
    /** 0 for the primary outcome, then the secondary questions in order. */
    val position: Int,
    val prompt: String,
    /** One of the [AnswerKind] values. */
    @ColumnInfo(name = "answer_type") val answerType: String,
    @ColumnInfo(name = "rating_min") val ratingMin: Int?,
    @ColumnInfo(name = "rating_max") val ratingMax: Int?,
    @ColumnInfo(name = "rating_min_label") val ratingMinLabel: String?,
    @ColumnInfo(name = "rating_max_label") val ratingMaxLabel: String?,
    @ColumnInfo(name = "number_unit") val numberUnit: String?,
    @ColumnInfo(name = "number_decimals") val numberDecimals: Int?,
    @ColumnInfo(name = "number_min") val numberMin: Double?,
    @ColumnInfo(name = "number_max") val numberMax: Double?,
)

/** How answer types are stored in `questions.answer_type` and `answers.kind`. */
internal object AnswerKind {
    const val RATING = "RATING"
    const val YES_NO = "YES_NO"
    const val NUMBER = "NUMBER"
    const val FREE_TEXT = "FREE_TEXT"
}

fun QuestionEntity.asExternalModel(): Question = Question(
    id = QuestionId(id),
    prompt = prompt,
    type = when (answerType) {
        AnswerKind.RATING -> AnswerType.Rating(
            min = requireNotNull(ratingMin) { "Rating question $id has no minimum" },
            max = requireNotNull(ratingMax) { "Rating question $id has no maximum" },
            minLabel = ratingMinLabel.orEmpty(),
            maxLabel = ratingMaxLabel.orEmpty(),
        )

        AnswerKind.YES_NO -> AnswerType.YesNo

        AnswerKind.NUMBER -> AnswerType.NumberWithUnit(
            unit = requireNotNull(numberUnit) { "Number question $id has no unit" },
            decimals = requireNotNull(numberDecimals) { "Number question $id has no decimals" },
            min = numberMin,
            max = numberMax,
        )

        AnswerKind.FREE_TEXT -> AnswerType.FreeText

        else -> error("Question $id has an unknown answer type: $answerType")
    },
)

fun Question.asEntity(experimentId: ExperimentId, position: Int): QuestionEntity {
    val rating = type as? AnswerType.Rating
    val number = type as? AnswerType.NumberWithUnit
    return QuestionEntity(
        id = id.value,
        experimentId = experimentId.value,
        position = position,
        prompt = prompt,
        answerType = type.kind,
        ratingMin = rating?.min,
        ratingMax = rating?.max,
        ratingMinLabel = rating?.minLabel,
        ratingMaxLabel = rating?.maxLabel,
        numberUnit = number?.unit,
        numberDecimals = number?.decimals,
        numberMin = number?.min,
        numberMax = number?.max,
    )
}

private val AnswerType.kind: String
    get() = when (this) {
        is AnswerType.Rating -> AnswerKind.RATING
        AnswerType.YesNo -> AnswerKind.YES_NO
        is AnswerType.NumberWithUnit -> AnswerKind.NUMBER
        AnswerType.FreeText -> AnswerKind.FREE_TEXT
    }
