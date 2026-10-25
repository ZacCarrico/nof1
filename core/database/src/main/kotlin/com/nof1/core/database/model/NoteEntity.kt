package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.PrimaryKey
import com.nof1.core.model.ExperimentId
import com.nof1.core.model.HypothesisId
import com.nof1.core.model.Note
import com.nof1.core.model.NoteId
import com.nof1.core.model.NoteParent
import com.nof1.core.model.ProjectId
import java.time.Instant

/**
 * A note. Exactly one of the parent columns is set; a separate column per parent type lets the
 * database delete notes along with their parent.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["project_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = HypothesisEntity::class,
            parentColumns = ["id"],
            childColumns = ["hypothesis_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ExperimentEntity::class,
            parentColumns = ["id"],
            childColumns = ["experiment_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("project_id"), Index("hypothesis_id"), Index("experiment_id")],
)
data class NoteEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "project_id") val projectId: String?,
    @ColumnInfo(name = "hypothesis_id") val hypothesisId: String?,
    @ColumnInfo(name = "experiment_id") val experimentId: String?,
    val text: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

fun NoteEntity.asExternalModel(): Note = Note(
    id = NoteId(id),
    parent = when {
        projectId != null -> NoteParent.OfProject(ProjectId(projectId))
        hypothesisId != null -> NoteParent.OfHypothesis(HypothesisId(hypothesisId))
        experimentId != null -> NoteParent.OfExperiment(ExperimentId(experimentId))
        else -> error("Note $id has no parent")
    },
    text = text,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
)

fun Note.asEntity(): NoteEntity = NoteEntity(
    id = id.value,
    projectId = (parent as? NoteParent.OfProject)?.projectId?.value,
    hypothesisId = (parent as? NoteParent.OfHypothesis)?.hypothesisId?.value,
    experimentId = (parent as? NoteParent.OfExperiment)?.experimentId?.value,
    text = text,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
)
