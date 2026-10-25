package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/** A check-in without its answers, which are in [AnswerEntity]. One per study day. */
@Entity(
    tableName = "check_ins",
    foreignKeys = [
        ForeignKey(
            entity = ExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["experiment_id", "study_day"], unique = true)],
)
data class CheckInEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "experiment_id") val experimentId: String,
    @ColumnInfo(name = "study_day") val studyDay: String,
    @ColumnInfo(name = "followed_plan") val followedPlan: Boolean?,
    @ColumnInfo(name = "recorded_at") val recordedAt: Long,
    /** The ID of the time zone the check-in was recorded in, such as "America/New_York". */
    @ColumnInfo(name = "time_zone") val timeZone: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
