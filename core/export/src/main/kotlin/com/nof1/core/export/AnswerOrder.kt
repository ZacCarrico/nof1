package com.nof1.core.export

import com.nof1.core.model.Answer
import com.nof1.core.model.CheckIn
import com.nof1.core.model.QuestionId

/**
 * The check-in's answers in [questionOrder], then any answers to questions not in it, by question
 * ID, so exports list answers the same way every time.
 */
internal fun CheckIn.answersInOrder(
    questionOrder: List<QuestionId>,
): List<Pair<QuestionId, Answer>> {
    val positions = questionOrder.withIndex().associate { (position, id) -> id to position }
    return answers.toList().sortedWith(
        compareBy({ positions[it.first] ?: Int.MAX_VALUE }, { it.first.value }),
    )
}
