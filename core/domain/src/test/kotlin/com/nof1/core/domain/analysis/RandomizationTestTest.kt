package com.nof1.core.domain.analysis

import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RandomizationTestTest {
    // Expected values in the worked examples come from an independent brute-force Python script.

    @Test
    fun `five pairs all favoring the intervention give the smallest possible p-value`() {
        val result = RandomizationTest.run(listOf(1.0, 2.0, 3.0, 4.0, 5.0))

        assertClose(3.0, result.estimate)
        assertClose(1.0 / 32, result.pValueImprovement)
        assertClose(1.0, result.pValueWorsening)
        assertEquals(
            ConfidenceInterval(lower = 1.0, upper = 5.0, coverage = 0.9375),
            result.interval,
        )
        assertTrue(result.isExact)
    }

    @Test
    fun `one worse pair among six`() {
        val result = RandomizationTest.run(listOf(-1.0, 2.0, 3.0, 4.0, 5.0, 6.0))

        assertClose(19.0 / 6, result.estimate)
        assertClose(2.0 / 64, result.pValueImprovement)
        assertClose(63.0 / 64, result.pValueWorsening)
        assertClose(1.0, result.interval.lower)
        assertClose(5.0, result.interval.upper)
        assertClose(0.90625, result.interval.coverage)
    }

    @Test
    fun `seven pairs with mixed results`() {
        val result = RandomizationTest.run(listOf(1.2, 0.4, -0.3, 2.1, 0.9, 1.6, 0.2))

        assertClose(0.8714285714285716, result.estimate)
        assertClose(3.0 / 128, result.pValueImprovement)
        assertClose(126.0 / 128, result.pValueWorsening)
        assertClose(0.26666666666666666, result.interval.lower)
        assertClose(1.5333333333333334, result.interval.upper)
    }

    @Test
    fun `five pairs all favoring the baseline`() {
        val result = RandomizationTest.run(listOf(-0.5, -1.0, -0.2, -0.8, -1.5))

        assertClose(-0.8, result.estimate)
        assertClose(1.0, result.pValueImprovement)
        assertClose(1.0 / 32, result.pValueWorsening)
        assertClose(-1.5, result.interval.lower)
        assertClose(-0.2, result.interval.upper)
    }

    @Test
    fun `four pairs are too few to bound the effect`() {
        val result = RandomizationTest.run(listOf(0.5, -0.25, 1.5, 2.0))

        assertClose(0.125, result.pValueImprovement)
        assertEquals(Double.NEGATIVE_INFINITY, result.interval.lower)
        assertEquals(Double.POSITIVE_INFINITY, result.interval.upper)
        assertClose(1.0, result.interval.coverage)
    }

    @Test
    fun `matches the definition of the test on random data`() {
        val random = Random(20261024)
        repeat(60) {
            // One decimal place, so some subset averages tie exactly.
            val improvements = List(random.nextInt(1, 10)) { random.nextInt(-30, 31) / 10.0 }

            val result = RandomizationTest.run(improvements)
            val (lower, upper) = intervalFromDefinition(improvements, RandomizationTest.ALPHA)

            assertClose(
                pValueFromDefinition(improvements, 0.0, improvement = true),
                result.pValueImprovement,
            )
            assertClose(
                pValueFromDefinition(improvements, 0.0, improvement = false),
                result.pValueWorsening,
            )
            assertClose(lower, result.interval.lower)
            assertClose(upper, result.interval.upper)
        }
    }

    @Test
    fun `Monte Carlo sampling agrees with full enumeration`() {
        val random = Random(7)
        val improvements = List(16) { random.nextDouble(-1.0, 1.5) }

        val exact = RandomizationTest.run(improvements)
        val sampled = RandomizationTest.run(improvements, maxExactPairs = 0)

        assertTrue(exact.isExact)
        assertFalse(sampled.isExact)
        val samples = RandomizationTest.MONTE_CARLO_SAMPLES.toDouble()
        for ((exactP, sampledP) in listOf(
            exact.pValueImprovement to sampled.pValueImprovement,
            exact.pValueWorsening to sampled.pValueWorsening,
        )) {
            val allowed = 4 * sqrt(exactP * (1 - exactP) / samples) + 2 / samples
            assertTrue(abs(exactP - sampledP) <= allowed, "exact $exactP, sampled $sampledP")
        }
        // Each sampled bound should cut off about α of the exact reference distribution.
        val means = allSubsetMeans(improvements)
        val belowLower = means.count { it < sampled.interval.lower } / means.size.toDouble()
        val aboveUpper = means.count { it > sampled.interval.upper } / means.size.toDouble()
        assertClose(RandomizationTest.ALPHA, belowLower, tolerance = 0.005)
        assertClose(RandomizationTest.ALPHA, aboveUpper, tolerance = 0.005)
    }

    @Test
    fun `Monte Carlo results are reproducible`() {
        val improvements = List(25) { it % 7 - 2.5 }

        assertEquals(RandomizationTest.run(improvements), RandomizationTest.run(improvements))
    }

    @Test
    fun `the interval covers the true effect at least 90 percent of the time`() {
        // The simulated periods drift upward over time; random pair orders keep that from biasing.
        val trueEffect = 0.7
        val trials = 4_000
        val random = Random(2026)

        val covered = (1..trials).count {
            val interval = RandomizationTest.run(simulatedImprovements(random, trueEffect)).interval
            trueEffect >= interval.lower && trueEffect <= interval.upper
        }

        // With 6 pairs the exact coverage is 1 - 2 * 3/64 = 0.906.
        assertTrue(
            covered / trials.toDouble() >= 0.885,
            "coverage was ${covered / trials.toDouble()}",
        )
    }

    @Test
    fun `a change with no effect rarely looks like an improvement despite a time trend`() {
        val trials = 4_000
        val random = Random(1)

        val falseImprovements = (1..trials).count {
            RandomizationTest.run(simulatedImprovements(random, trueEffect = 0.0)).interval.lower >
                0.0
        }

        // The exact rate is 3/64 = 0.047.
        assertTrue(
            falseImprovements / trials.toDouble() <= 0.06,
            "rate was ${falseImprovements / trials.toDouble()}",
        )
    }

    @Test
    fun `rejects invalid input`() {
        assertFailsWith<IllegalArgumentException> { RandomizationTest.run(emptyList()) }
        assertFailsWith<IllegalArgumentException> { RandomizationTest.run(listOf(1.0, Double.NaN)) }
        assertFailsWith<IllegalArgumentException> {
            RandomizationTest.run(listOf(1.0), alpha = 0.6)
        }
    }

    /**
     * Six pairs whose periods drift upward over time, with noise. The order of each pair is random,
     * and the intervention adds [trueEffect] to its period.
     */
    private fun simulatedImprovements(random: Random, trueEffect: Double): List<Double> =
        List(6) { pair ->
            val firstPeriod = 5.0 + 0.4 * (2 * pair) + random.nextDouble(-1.0, 1.0)
            val secondPeriod = 5.0 + 0.4 * (2 * pair + 1) + random.nextDouble(-1.0, 1.0)
            if (random.nextBoolean()) {
                secondPeriod + trueEffect - firstPeriod
            } else {
                firstPeriod + trueEffect - secondPeriod
            }
        }

    /** The one-sided p-value straight from its definition: enumerate every sign pattern. */
    private fun pValueFromDefinition(
        improvements: List<Double>,
        effect: Double,
        improvement: Boolean,
    ): Double {
        val shifted = improvements.map { it - effect }
        val observed = shifted.sum()
        val patterns = 1 shl shifted.size
        val atLeastAsExtreme = (0 until patterns).count { pattern ->
            val total = shifted.indices.sumOf { i ->
                if (pattern shr i and 1 ==
                    1
                ) {
                    -shifted[i]
                } else {
                    shifted[i]
                }
            }
            if (improvement) total >= observed - TIE else total <= observed + TIE
        }
        return atLeastAsExtreme.toDouble() / patterns
    }

    /** The smallest and largest effects the test doesn't reject, found by testing candidates. */
    private fun intervalFromDefinition(
        improvements: List<Double>,
        alpha: Double,
    ): Pair<Double, Double> {
        val candidates = allSubsetMeans(improvements).sorted()
        fun lowerAccepts(effect: Double) =
            pValueFromDefinition(improvements, effect, improvement = true) > alpha
        fun upperAccepts(effect: Double) =
            pValueFromDefinition(improvements, effect, improvement = false) > alpha
        val lower = if (lowerAccepts(candidates.first() - 1.0)) {
            Double.NEGATIVE_INFINITY
        } else {
            candidates.first(::lowerAccepts)
        }
        val upper = if (upperAccepts(candidates.last() + 1.0)) {
            Double.POSITIVE_INFINITY
        } else {
            candidates.last(::upperAccepts)
        }
        return lower to upper
    }

    private fun allSubsetMeans(values: List<Double>): List<Double> =
        (1 until (1 shl values.size)).map { subset ->
            val members = values.indices.filter { subset shr it and 1 == 1 }
            members.sumOf { values[it] } / members.size
        }

    private fun assertClose(expected: Double, actual: Double, tolerance: Double = 1e-9) {
        if (expected.isInfinite()) {
            assertEquals(expected, actual)
        } else {
            assertTrue(abs(expected - actual) <= tolerance, "expected $expected, was $actual")
        }
    }

    private companion object {
        const val TIE = 1e-9
    }
}
