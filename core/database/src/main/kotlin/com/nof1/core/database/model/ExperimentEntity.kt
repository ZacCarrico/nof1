package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * An experiment without its questions, which are in [QuestionEntity]. The schedule columns
 * (`start_date`, `pair_orders`, `schedule_seed`) are all null for a draft and all set once it
 * starts.
 */
@Entity(
    tableName = "experiments",
    foreignKeys = [
        ForeignKey(
            entity = HypothesisEntity::class,
            parentColumns = ["id"],
            childColumns = ["hypothesis_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("hypothesis_id")],
)
data class ExperimentEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "hypothesis_id") val hypothesisId: String,
    val title: String,
    @ColumnInfo(name = "baseline_name") val baselineName: String,
    @ColumnInfo(name = "baseline_instructions") val baselineInstructions: String,
    @ColumnInfo(name = "intervention_name") val interventionName: String,
    @ColumnInfo(name = "intervention_instructions") val interventionInstructions: String,
    @ColumnInfo(name = "better_direction") val betterDirection: String,
    @ColumnInfo(name = "minimum_meaningful_change") val minimumMeaningfulChange: Double,
    @ColumnInfo(name = "asks_adherence") val asksAdherence: Boolean,
    @ColumnInfo(name = "period_length_days") val periodLengthDays: Int,
    @ColumnInfo(name = "settling_in_days") val settlingInDays: Int,
    @ColumnInfo(name = "pair_count") val pairCount: Int,
    /** Local time of day, such as "21:00". */
    @ColumnInfo(name = "check_in_time") val checkInTime: String,
    @ColumnInfo(name = "check_in_day") val checkInDay: String,
    @ColumnInfo(name = "start_date") val startDate: String?,
    /** Comma-separated, one per pair, such as "BASELINE_FIRST,INTERVENTION_FIRST". */
    @ColumnInfo(name = "pair_orders") val pairOrders: String?,
    @ColumnInfo(name = "schedule_seed") val scheduleSeed: Long?,
    @ColumnInfo(name = "stopped_on") val stoppedOn: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)
