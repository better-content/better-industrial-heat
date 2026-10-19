package com.bettercontent.betterindustrialheat.food

/** Amplifiers are zero-based; the gameplay scales use the displayed, one-based level. */
internal object FoodDrainPolicy {
    fun hydrationPerSecond(amplifier: Int): Double = (amplifier.coerceAtLeast(0) + 1.0) * 20.0 / 800.0

    fun nutrientFractionPerSecond(amplifier: Int): Double = (amplifier.coerceAtLeast(0) + 1.0) / 100.0

    fun hydrationWithdrawal(pending: Double, amplifier: Int): Pair<Int, Double> {
        val accumulated = pending + hydrationPerSecond(amplifier)
        val whole = kotlin.math.floor(accumulated + 1.0e-9).toInt()
        return whole to (accumulated - whole).coerceAtLeast(0.0)
    }

    fun remainingNutrient(value: Float, amplifier: Int): Float =
        (value - nutrientFractionPerSecond(amplifier).toFloat()).coerceAtLeast(0f)
}
