package com.nof1.data.model

/**
 * Type aliases to replace Room entities with Firebase entities.
 * This allows for easier migration without changing all references throughout the codebase.
 */

// Primary model aliases
typealias Project = FirebaseProject
typealias Hypothesis = FirebaseHypothesis  
typealias Experiment = FirebaseExperiment
typealias LogEntry = FirebaseLogEntry
typealias Note = FirebaseNote
typealias ReminderSettings = FirebaseReminderSettings

// Relationship model aliases
typealias ProjectWithHypotheses = FirebaseProjectWithHypotheses
typealias ProjectWithReminders = FirebaseProjectWithReminders
typealias HypothesisWithExperiments = FirebaseHypothesisWithExperiments
typealias HypothesisWithNotes = FirebaseHypothesisWithNotes
typealias HypothesisWithReminders = FirebaseHypothesisWithReminders
typealias ExperimentWithLogs = FirebaseExperimentWithLogs
typealias ProjectWithHypothesesAndExperiments = FirebaseProjectWithHypothesesAndExperiments 