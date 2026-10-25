package com.nof1.core.model

import java.time.Instant

/** Something the user wants to improve, such as "Sleep better". */
data class Project(
    val id: ProjectId,
    val name: String,
    val goal: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null,
)

/** A belief about what affects a project's goal, tested by experiments. */
data class Hypothesis(
    val id: HypothesisId,
    val projectId: ProjectId,
    val statement: String,
    val details: String = "",
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null,
)
