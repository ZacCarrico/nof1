package com.nof1.core.domain.analysis

import kotlin.test.Test
import kotlin.test.assertEquals

class VerdictTest {
    private fun verdict(estimate: Double, lower: Double, upper: Double, pairs: Int = 6) =
        Verdicts.decide(
            RandomizationTestResult(
                pairCount = pairs,
                estimate = estimate,
                interval = ConfidenceInterval(lower, upper, coverage = 0.9),
                pValueImprovement = 0.5,
                pValueWorsening = 0.5,
                isExact = true,
            ),
            minimumMeaningfulChange = 1.0,
        )

    @Test
    fun `fewer than five pairs is inconclusive`() {
        val tooFew = VerdictResult(Verdict.INCONCLUSIVE, InconclusiveReason.TOO_FEW_PAIRS)

        assertEquals(tooFew, verdict(estimate = 2.0, lower = 1.0, upper = 3.0, pairs = 4))
        assertEquals(tooFew, Verdicts.decide(test = null, minimumMeaningfulChange = 1.0))
    }

    @Test
    fun `helped when the interval is above zero and the estimate meets the minimum change`() {
        assertEquals(
            VerdictResult(Verdict.HELPED),
            verdict(estimate = 1.2, lower = 0.5, upper = 2.0),
        )
        assertEquals(
            VerdictResult(Verdict.HELPED),
            verdict(estimate = 1.0 - 1e-12, lower = 0.2, upper = 1.8),
        )
    }

    @Test
    fun `helped a little when the improvement is real but below the minimum change`() {
        assertEquals(
            VerdictResult(Verdict.HELPED_A_LITTLE),
            verdict(estimate = 0.6, lower = 0.1, upper = 1.0),
        )
    }

    @Test
    fun `made it worse when the interval is below zero`() {
        assertEquals(
            VerdictResult(Verdict.MADE_IT_WORSE),
            verdict(estimate = -0.4, lower = -2.0, upper = -0.1),
        )
    }

    @Test
    fun `no meaningful effect when the interval fits inside the minimum change`() {
        assertEquals(
            VerdictResult(Verdict.NO_MEANINGFUL_EFFECT),
            verdict(estimate = 0.1, lower = -0.4, upper = 0.6),
        )
    }

    @Test
    fun `inconclusive when the interval is too wide`() {
        val uncertain = VerdictResult(Verdict.INCONCLUSIVE, InconclusiveReason.TOO_UNCERTAIN)

        assertEquals(uncertain, verdict(estimate = 0.6, lower = -0.4, upper = 1.6))
        // An interval that only touches zero or the minimum change supports no conclusion.
        assertEquals(uncertain, verdict(estimate = 0.5, lower = 0.0, upper = 1.0))
        assertEquals(uncertain, verdict(estimate = 1.2, lower = 0.0, upper = 2.4))
        assertEquals(uncertain, verdict(estimate = -1.2, lower = -2.4, upper = 0.0))
    }
}
