package com.nof1.core.model

import java.time.Instant

/** A free-form text note, such as a possible confounder ("late coffee today"). */
data class Note(
    val id: NoteId,
    val parent: NoteParent,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/** What a [Note] is attached to. */
sealed interface NoteParent {
    data class OfProject(val projectId: ProjectId) : NoteParent

    data class OfHypothesis(val hypothesisId: HypothesisId) : NoteParent

    data class OfExperiment(val experimentId: ExperimentId) : NoteParent
}
