package com.nof1.data.model

/**
 * Firebase-compatible relation models to replace Room's @Relation functionality.
 * These models will be populated by making multiple Firebase queries.
 */

/**
 * Represents a project with its associated hypotheses.
 */
data class FirebaseProjectWithHypotheses(
    val project: FirebaseProject,
    val hypotheses: List<FirebaseHypothesis>
)

/**
 * Represents a project with its reminder settings.
 */
data class FirebaseProjectWithReminders(
    val project: FirebaseProject,
    val reminderSettings: List<FirebaseReminderSettings>
)

/**
 * Represents a hypothesis with its associated experiments.
 */
data class FirebaseHypothesisWithExperiments(
    val hypothesis: FirebaseHypothesis,
    val experiments: List<FirebaseExperiment>
)

/**
 * Represents a hypothesis with its notes.
 */
data class FirebaseHypothesisWithNotes(
    val hypothesis: FirebaseHypothesis,
    val notes: List<FirebaseNote>
)

/**
 * Represents a hypothesis with its reminder settings.
 */
data class FirebaseHypothesisWithReminders(
    val hypothesis: FirebaseHypothesis,
    val reminderSettings: List<FirebaseReminderSettings>
)

/**
 * Represents an experiment with its associated log entries.
 */
data class FirebaseExperimentWithLogs(
    val experiment: FirebaseExperiment,
    val logEntries: List<FirebaseLogEntry>
)

/**
 * Represents a complete project hierarchy with all nested data.
 */
data class FirebaseProjectWithHypothesesAndExperiments(
    val project: FirebaseProject,
    val hypothesesWithExperiments: List<FirebaseHypothesisWithExperiments>
)

/**
 * Represents a complete project with all its data.
 */
data class FirebaseProjectComplete(
    val project: FirebaseProject,
    val hypotheses: List<FirebaseHypothesis>,
    val experiments: List<FirebaseExperiment>,
    val logEntries: List<FirebaseLogEntry>,
    val notes: List<FirebaseNote>,
    val reminderSettings: List<FirebaseReminderSettings>
) 