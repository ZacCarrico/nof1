package com.nof1.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents a hypothesis within a project.
 * A hypothesis is a refinement of the project's goal that can be tested through experiments.
 */
@Entity(
    tableName = "hypotheses",
    foreignKeys = [
        ForeignKey(
            entity = Project::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")]
)
data class Hypothesis(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val projectId: Long,
    val name: String,
    val description: String,
    val isArchived: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) 