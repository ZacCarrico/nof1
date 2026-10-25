package com.nof1.core.model

import java.util.UUID

// IDs are random UUIDs, so records created on different devices won't collide once sync exists.
// Each entity has its own ID type so they can't be mixed up.

@JvmInline
value class ProjectId(val value: String) {
    companion object {
        fun random() = ProjectId(randomIdValue())
    }
}

@JvmInline
value class HypothesisId(val value: String) {
    companion object {
        fun random() = HypothesisId(randomIdValue())
    }
}

@JvmInline
value class ExperimentId(val value: String) {
    companion object {
        fun random() = ExperimentId(randomIdValue())
    }
}

@JvmInline
value class QuestionId(val value: String) {
    companion object {
        fun random() = QuestionId(randomIdValue())
    }
}

@JvmInline
value class CheckInId(val value: String) {
    companion object {
        fun random() = CheckInId(randomIdValue())
    }
}

@JvmInline
value class NoteId(val value: String) {
    companion object {
        fun random() = NoteId(randomIdValue())
    }
}

private fun randomIdValue(): String = UUID.randomUUID().toString()
