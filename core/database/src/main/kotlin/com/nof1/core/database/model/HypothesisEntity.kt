package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.nof1.core.model.Hypothesis
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.ProjectId
import java.time.Instant

@Entity(
    tableName = "hypotheses",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_id")],
)
data class HypothesisEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String,
    val statement: String,
    val details: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)

fun HypothesisEntity.asExternalModel(): Hypothesis = Hypothesis(
    id = HypothesisId(id),
    projectId = ProjectId(projectId),
    statement = statement,
    details = details,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
)

fun Hypothesis.asEntity(): HypothesisEntity = HypothesisEntity(
    id = id.value,
    projectId = projectId.value,
    statement = statement,
    details = details,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    archivedAt = archivedAt?.toEpochMilli(),
)
