package com.bettercontent.betterindustrialheat.food

/** Pure active-time ageing rules shared by normal settles and count-weighted stack transfers. */
internal object FoodAgePolicy {
    /**
     * Advances decay for [elapsedTicks] of active time. One settle can cross at most one
     * stage boundary: the excess elapsed time is dropped, so a pause, an unloaded chunk, or
     * a leaked timestamp can never one-step food from fresh to ruined.
     */
    fun advanceDecay(decay: Double, elapsedTicks: Long, preservationRate: Double, lifetimeDays: Double?): Double {
        if (lifetimeDays == null || elapsedTicks <= 0L) return decay
        val added = elapsedTicks.toDouble() * preservationRate.coerceIn(0.0, 1.0) / (lifetimeDays * 24_000.0)
        return (decay + added).coerceIn(0.0, nextStageBoundary(decay))
    }

    /** The stage boundary this settle may reach: 1.0 stale, 1.5 spoiled, 2.0 rotten, 2.5 converted. */
    fun nextStageBoundary(decay: Double): Double = when {
        decay < 1.0 -> 1.0
        decay < 1.5 -> 1.5
        decay < 2.0 -> 2.0
        else -> 2.5
    }

    fun stage(decay: Double): FoodThermalService.Stage {
        // Keep the epsilon used by the persisted path so exact tick boundaries survive rounding.
        val value = decay + 1.0e-10
        return when {
            value >= 2.5 -> FoodThermalService.Stage.CONVERTED
            value >= 2.0 -> FoodThermalService.Stage.ROTTEN
            value >= 1.5 -> FoodThermalService.Stage.SPOILED
            value >= 1.0 -> FoodThermalService.Stage.STALE
            else -> FoodThermalService.Stage.FRESH
        }
    }

    fun remainsFresh(decay: Double, elapsedTicks: Long, preservationRate: Double, lifetimeDays: Double?): Boolean =
        stage(advanceDecay(decay, elapsedTicks, preservationRate, lifetimeDays)) == FoodThermalService.Stage.FRESH
}
