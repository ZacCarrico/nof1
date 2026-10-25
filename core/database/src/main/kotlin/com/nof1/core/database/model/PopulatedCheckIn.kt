package com.nof1.core.database.model

import androidx.room3.Embedded
import androidx.room3.Relation
import com.nof1.core.model.CheckIn
import com.nof1.core.model.CheckInId
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.QuestionId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** A check-in with its answers. */
data class PopulatedCheckIn(
    @Embedded val checkIn: CheckInEntity,
    @Relation(parentColumns = ["id"], entityColumns = ["check_in_id"])
    val answers: List<AnswerEntity>,
)

fun PopulatedCheckIn.asExternalModel(): CheckIn = CheckIn(
    id = CheckInId(checkIn.id),
    experimentId = ExperimentId(checkIn.experimentId),
    studyDay = LocalDate.parse(checkIn.studyDay),
    followedPlan = checkIn.followedPlan,
    answers = answers.associate { QuestionId(it.questionId) to it.asExternalModel() },
    recordedAt = Instant.ofEpochMilli(checkIn.recordedAt),
    zone = ZoneId.of(checkIn.timeZone),
    updatedAt = Instant.ofEpochMilli(checkIn.updatedAt),
)

fun CheckIn.asEntity(): CheckInEntity = CheckInEntity(
    id = id.value,
    experimentId = experimentId.value,
    studyDay = studyDay.toString(),
    followedPlan = followedPlan,
    recordedAt = recordedAt.toEpochMilli(),
    timeZone = zone.id,
    updatedAt = updatedAt.toEpochMilli(),
)

fun CheckIn.answerEntities(): List<AnswerEntity> =
    answers.map { (questionId, answer) -> answer.asEntity(id, questionId) }
