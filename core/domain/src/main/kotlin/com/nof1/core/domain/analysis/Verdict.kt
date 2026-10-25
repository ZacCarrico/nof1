package com.nof1.core.domain.analysis

enum class Verdict {
    HELPED,

    /** A real improvement, but smaller than the change the user said would matter. */
    HELPED_A_LITTLE,

    NO_MEANINGFUL_EFFECT,
    MADE_IT_WORSE,
    INCONCLUSIVE,
}

enum class InconclusiveReason {
    /** Fewer complete pairs than the test needs to rule out chance. */
    TOO_FEW_PAIRS,

    /** The confidence interval is too wide to decide. */
    TOO_UNCERTAIN,
}

data class VerdictResult(val verdict: Verdict, val inconclusiveReason: InconclusiveReason? = null)

object Verdicts {
    /**
     * With K pairs there are only 2^K possible orders. Below 5 pairs, even a perfectly consistent
     * result can't reach the 5% level (4 pairs: 1/16 ≈ 0.06).
     */
    const val MIN_COMPLETE_PAIRS = 5

    private const val TOLERANCE = 1e-9

    /**
     * Applies the rules in order, using the test's confidence interval and the minimum meaningful
     * change; the first match wins.
     */
    fun decide(test: RandomizationTestResult?, minimumMeaningfulChange: Double): VerdictResult {
        if (test == null || test.pairCount < MIN_COMPLETE_PAIRS) {
            return VerdictResult(Verdict.INCONCLUSIVE, InconclusiveReason.TOO_FEW_PAIRS)
        }
        val (lower, upper) = test.interval
        val meetsMinimum = test.estimate >= minimumMeaningfulChange - TOLERANCE
        val withinMinimum = lower > -minimumMeaningfulChange && upper < minimumMeaningfulChange
        return when {
            lower > 0.0 && meetsMinimum -> VerdictResult(Verdict.HELPED)
            lower > 0.0 -> VerdictResult(Verdict.HELPED_A_LITTLE)
            upper < 0.0 -> VerdictResult(Verdict.MADE_IT_WORSE)
            withinMinimum -> VerdictResult(Verdict.NO_MEANINGFUL_EFFECT)
            else -> VerdictResult(Verdict.INCONCLUSIVE, InconclusiveReason.TOO_UNCERTAIN)
        }
    }
}
