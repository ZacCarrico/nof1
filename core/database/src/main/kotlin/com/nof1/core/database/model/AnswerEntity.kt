package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import com.nof1.core.model.Answer
import com.nof1.core.model.CheckInId
import com.nof1.core.model.QuestionId

/**
 * One answer in a check-in. Only the value column for its [kind] is set.
 *
 * The question reference has no delete action on purpose: deleting a question that has answers
 * fails, unless the whole experiment is deleted with it.
 */
@Entity(
    tableName = "answers",
    primaryKeys = ["check_in_id", "question_id"],
    foreignKeys = [
        ForeignKey(
            entity = CheckInEntity::class,
            parentColumns = ["id"],
            childColumns = ["check_in_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = QuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
        ),
    ],
    indices = [Index("question_id")],
)
data class AnswerEntity(
    @ColumnInfo(name = "check_in_id") val checkInId: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    /** One of the [AnswerKind] values. */
    val kind: String,
    val rating: Int?,
    @ColumnInfo(name = "yes_no") val yesNo: Boolean?,
    val number: Double?,
    val text: String?,
)

fun AnswerEntity.asExternalModel(): Answer = when (kind) {
    AnswerKind.RATING -> Answer.Rating(requireNotNull(rating) { missingValue() })
    AnswerKind.YES_NO -> Answer.YesNo(requireNotNull(yesNo) { missingValue() })
    AnswerKind.NUMBER -> Answer.NumberWithUnit(requireNotNull(number) { missingValue() })
    AnswerKind.FREE_TEXT -> Answer.FreeText(requireNotNull(text) { missingValue() })
    else -> error("Answer to $questionId in check-in $checkInId has an unknown kind: $kind")
}

private fun AnswerEntity.missingValue() =
    "Answer to $questionId in check-in $checkInId has no $kind value"

fun Answer.asEntity(checkInId: CheckInId, questionId: QuestionId): AnswerEntity = AnswerEntity(
    checkInId = checkInId.value,
    questionId = questionId.value,
    kind = when (this) {
        is Answer.Rating -> AnswerKind.RATING
        is Answer.YesNo -> AnswerKind.YES_NO
        is Answer.NumberWithUnit -> AnswerKind.NUMBER
        is Answer.FreeText -> AnswerKind.FREE_TEXT
    },
    rating = (this as? Answer.Rating)?.value,
    yesNo = (this as? Answer.YesNo)?.value,
    number = (this as? Answer.NumberWithUnit)?.value,
    text = (this as? Answer.FreeText)?.text,
)
