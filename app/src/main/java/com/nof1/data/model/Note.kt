package com.nof1.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Represents a note attached to a hypothesis.
 * Each note has a timestamp and can be used to track thoughts, updates, or observations.
 */
@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = Hypothesis::class,
            parentColumns = ["id"],
            childColumns = ["hypothesisId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("hypothesisId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val hypothesisId: Long,
    val content: String,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
) 