package com.nof1.data.model

import androidx.room.Embedded
import androidx.room.Relation

/**
 * Represents a project with its associated hypotheses.
 */
data class ProjectWithHypotheses(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val hypotheses: List<Hypothesis>
)

/**
 * Represents a project with its reminder settings.
 */
data class ProjectWithReminders(
    @Embedded val project: Project,
    @Relation(
        parentColumn = "id",
        entityColumn = "entityId"
    )
    val reminderSettings: List<ReminderSettings>
)

/**
 * Represents a hypothesis with its associated experiments.
 */
data class HypothesisWithExperiments(
    @Embedded val hypothesis: Hypothesis,
    @Relation(
        parentColumn = "id",
        entityColumn = "hypothesisId"
    )
    val experiments: List<Experiment>
)

/**
 * Represents a hypothesis with its reminder settings.
 */
data class HypothesisWithReminders(
    @Embedded val hypothesis: Hypothesis,
    @Relation(
        parentColumn = "id",
        entityColumn = "entityId"
    )
    val reminderSettings: List<ReminderSettings>
)

/**
 * Represents an experiment with its associated log entries.
 */
data class ExperimentWithLogs(
    @Embedded val experiment: Experiment,
    @Relation(
        parentColumn = "id",
        entityColumn = "experimentId"
    )
    val logEntries: List<LogEntry>
)

/**
 * Represents a complete project hierarchy with all nested data.
 */
data class ProjectWithHypothesesAndExperiments(
    @Embedded val project: Project,
    @Relation(
        entity = Hypothesis::class,
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val hypothesesWithExperiments: List<HypothesisWithExperiments>
) 