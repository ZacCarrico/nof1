package com.nof1.core.model

/** A question asked at every check-in of an experiment. */
data class Question(val id: QuestionId, val prompt: String, val type: AnswerType)

/** The kind of answer a [Question] takes. */
sealed interface AnswerType {
    /** Whether answers can be averaged and compared between periods. */
    val isQuantitative: Boolean

    /** Whether [answer] is of this type and within its allowed range. */
    fun accepts(answer: Answer): Boolean

    /** A whole number on a scale, such as 1–10. */
    data class Rating(
        val min: Int = 1,
        val max: Int = 10,
        val minLabel: String = "",
        val maxLabel: String = "",
    ) : AnswerType {
        init {
            require(min < max) { "Rating min ($min) must be below max ($max)" }
        }

        override val isQuantitative: Boolean get() = true

        override fun accepts(answer: Answer): Boolean =
            answer is Answer.Rating && answer.value in min..max
    }

    data object YesNo : AnswerType {
        override val isQuantitative: Boolean get() = true

        override fun accepts(answer: Answer): Boolean = answer is Answer.YesNo
    }

    /** A measured amount, such as hours slept or milligrams of caffeine. */
    data class NumberWithUnit(
        val unit: String,
        val decimals: Int = 0,
        val min: Double? = null,
        val max: Double? = null,
    ) : AnswerType {
        init {
            require(decimals in 0..MAX_DECIMALS) { "decimals must be in 0..$MAX_DECIMALS" }
            require(min == null || max == null || min < max) {
                "min ($min) must be below max ($max)"
            }
        }

        override val isQuantitative: Boolean get() = true

        override fun accepts(answer: Answer): Boolean = answer is Answer.NumberWithUnit &&
            answer.value.isFinite() &&
            (min == null || answer.value >= min) &&
            (max == null || answer.value <= max)

        companion object {
            const val MAX_DECIMALS = 3
        }
    }

    data object FreeText : AnswerType {
        override val isQuantitative: Boolean get() = false

        override fun accepts(answer: Answer): Boolean = answer is Answer.FreeText
    }
}

/** A single answer to a [Question]. */
sealed interface Answer {
    data class Rating(val value: Int) : Answer

    data class YesNo(val value: Boolean) : Answer

    data class NumberWithUnit(val value: Double) : Answer

    data class FreeText(val text: String) : Answer
}
