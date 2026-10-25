package com.nof1.core.domain.analysis

import com.nof1.core.domain.random.SplitMix64
import kotlin.math.abs
import kotlin.math.floor

/** A confidence interval and the minimum share of the time it contains the true effect. */
data class ConfidenceInterval(val lower: Double, val upper: Double, val coverage: Double)

data class RandomizationTestResult(
    val pairCount: Int,
    /** The average improvement per pair. */
    val estimate: Double,
    val interval: ConfidenceInterval,
    /** One-sided p-value for "the intervention helped". */
    val pValueImprovement: Double,
    /** One-sided p-value for "the intervention made things worse". */
    val pValueWorsening: Double,
    /** False when the reference distribution was sampled instead of fully enumerated. */
    val isExact: Boolean,
)

/**
 * The randomization test for an experiment made of randomized pairs, and the confidence interval
 * from inverting it.
 *
 * Each complete pair contributes an improvement: its intervention period's value minus its baseline
 * period's, with the sign flipped when lower values are better. If the intervention had no effect,
 * the pair could just as well have run in the opposite order with the same daily values, which only
 * flips the sign of its improvement. All 2^K sign patterns are then equally likely, which gives the
 * exact reference distribution of the average improvement. Only the random order is assumed; daily
 * values don't need to be normal or independent.
 *
 * Assuming the intervention shifts values by a roughly constant amount, the effects the test
 * doesn't reject form a confidence interval. Its bounds are order statistics of the averages of
 * all non-empty subsets of the improvements (Hartigan, 1969, "Using subsample values as typical
 * values").
 */
object RandomizationTest {
    /** One-sided significance level for each tail, so intervals cover at least 1 − 2α = 90%. */
    const val ALPHA = 0.05

    /** Above this many pairs, sign patterns are sampled instead of enumerated. */
    const val MAX_EXACT_PAIRS = 20

    const val MONTE_CARLO_SAMPLES = 100_000

    /** Fixed so the same data always gives the same result. The ASCII codes of "nof1". */
    const val MONTE_CARLO_SEED = 0x6E6F6631L

    private const val RELATIVE_TIE_TOLERANCE = 1e-9

    fun run(
        improvements: List<Double>,
        alpha: Double = ALPHA,
        maxExactPairs: Int = MAX_EXACT_PAIRS,
        monteCarloSamples: Int = MONTE_CARLO_SAMPLES,
        seed: Long = MONTE_CARLO_SEED,
    ): RandomizationTestResult {
        require(improvements.isNotEmpty()) { "Needs at least one complete pair" }
        require(improvements.all { it.isFinite() }) { "Improvements must be finite" }
        require(alpha > 0.0 && alpha < 0.5) { "alpha must be between 0 and 0.5, was $alpha" }
        require(maxExactPairs in 0..MAX_EXACT_PAIRS) {
            "maxExactPairs must be in 0..$MAX_EXACT_PAIRS"
        }
        require(monteCarloSamples > 0) { "monteCarloSamples must be positive" }

        val isExact = improvements.size <= maxExactPairs
        val reference = if (isExact) {
            allSubsetMeans(improvements)
        } else {
            sampledSubsetMeans(improvements, monteCarloSamples, seed)
        }
        // Subset averages within rounding error of zero count as ties, and ties count as at least
        // as extreme as the observed result, which keeps both p-values conservative.
        val tolerance = RELATIVE_TIE_TOLERANCE * maxOf(1.0, improvements.maxOf { abs(it) })
        val patterns = reference.patternCount.toDouble()
        val alwaysAtLeastAsExtreme = 1 + reference.identityCount

        val tailPatterns = floor(alpha * reference.patternCount).toInt()
        val tailRank = tailPatterns - reference.identityCount
        val means = reference.sortedMeans
        val interval = ConfidenceInterval(
            lower = if (tailRank >= 1) means[tailRank - 1] else Double.NEGATIVE_INFINITY,
            upper = if (tailRank >= 1) means[means.size - tailRank] else Double.POSITIVE_INFINITY,
            coverage = 1 - 2.0 * tailPatterns / patterns,
        )
        return RandomizationTestResult(
            pairCount = improvements.size,
            estimate = improvements.average(),
            interval = interval,
            pValueImprovement =
                (alwaysAtLeastAsExtreme + reference.countAtMost(tolerance)) / patterns,
            pValueWorsening =
                (alwaysAtLeastAsExtreme + reference.countAtLeast(-tolerance)) / patterns,
            isExact = isExact,
        )
    }

    /**
     * Flipping the signs of the pairs in a set S lowers the total improvement by twice their sum,
     * so a pattern is at least as favorable as the observed one exactly when S's average is ≤ 0.
     */
    private fun allSubsetMeans(improvements: List<Double>): ReferenceDistribution {
        val patternCount = 1 shl improvements.size
        val sums = DoubleArray(patternCount)
        val means = DoubleArray(patternCount - 1)
        for (subset in 1 until patternCount) {
            val lowestPair = subset.countTrailingZeroBits()
            sums[subset] = sums[subset and (subset - 1)] + improvements[lowestPair]
            means[subset - 1] = sums[subset] / subset.countOneBits()
        }
        means.sort()
        return ReferenceDistribution(means, identityCount = 0, patternCount = patternCount)
    }

    private fun sampledSubsetMeans(
        improvements: List<Double>,
        samples: Int,
        seed: Long,
    ): ReferenceDistribution {
        val random = SplitMix64(seed)
        val means = DoubleArray(samples)
        var meanCount = 0
        var identityCount = 0
        repeat(samples) {
            var sum = 0.0
            var flipped = 0
            var bits = 0L
            improvements.forEachIndexed { index, improvement ->
                if (index % Long.SIZE_BITS == 0) bits = random.nextLong()
                if (bits and 1L == 1L) {
                    sum += improvement
                    flipped++
                }
                bits = bits ushr 1
            }
            if (flipped == 0) identityCount++ else means[meanCount++] = sum / flipped
        }
        val sorted = means.copyOf(meanCount).apply { sort() }
        return ReferenceDistribution(sorted, identityCount, patternCount = samples + 1)
    }

    /**
     * @property sortedMeans the average improvement of the flipped pairs, for each pattern
     *   considered that flips at least one pair.
     * @property identityCount sampled patterns that flip nothing; always 0 when enumerating.
     * @property patternCount patterns in the reference distribution, including the observed one.
     */
    private class ReferenceDistribution(
        val sortedMeans: DoubleArray,
        val identityCount: Int,
        val patternCount: Int,
    ) {
        fun countAtMost(value: Double): Int = sortedMeans.indexOfFirstAbove(value)

        fun countAtLeast(value: Double): Int =
            sortedMeans.size - sortedMeans.indexOfFirstAtLeast(value)
    }
}

private fun DoubleArray.indexOfFirstAbove(value: Double): Int = binarySearchBoundary { it > value }

private fun DoubleArray.indexOfFirstAtLeast(value: Double): Int = binarySearchBoundary {
    it >= value
}

/** The first index where [predicate] holds, for a predicate that's false and then true. */
private inline fun DoubleArray.binarySearchBoundary(predicate: (Double) -> Boolean): Int {
    var low = 0
    var high = size
    while (low < high) {
        val middle = (low + high) ushr 1
        if (predicate(this[middle])) high = middle else low = middle + 1
    }
    return low
}
