package com.nof1.core.domain.random

/**
 * The SplitMix64 generator (Steele, Lea and Flood, 2014).
 *
 * Used instead of [kotlin.random.Random], whose sequence for a given seed may change between Kotlin
 * versions. This one is fixed, so a stored seed always reproduces the same results.
 */
internal class SplitMix64(seed: Long) {
    private var state = seed

    fun nextLong(): Long {
        state += GOLDEN_GAMMA
        var z = state
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    fun nextBoolean(): Boolean = nextLong() < 0

    private companion object {
        val GOLDEN_GAMMA = 0x9E3779B97F4A7C15uL.toLong()
        val MIX_1 = 0xBF58476D1CE4E5B9uL.toLong()
        val MIX_2 = 0x94D049BB133111EBuL.toLong()
    }
}
