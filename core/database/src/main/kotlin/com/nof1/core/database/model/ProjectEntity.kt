package com.nof1.core.database.model

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.nof1.core.model.Project
import com.nof1.core.model.ProjectId
import java.time.Instant

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val goal: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "archived_at") val archivedAt: Long?,
)

fun ProjectEntity.asExternalModel(): Project = Project(
    id = ProjectId(id),
    name = name,
    goal = goal,
    createdAt = Instant.ofEpochMilli(createdAt),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    archivedAt = archivedAt?.let(Instant::ofEpochMilli),
)

fun Project.asEntity(): ProjectEntity = ProjectEntity(
    id = id.value,
    name = name,
    goal = goal,
    createdAt = createdAt.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    archivedAt = archivedAt?.toEpochMilli(),
)
