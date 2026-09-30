package com.bettercontent.betterindustrialheat.food

/** Pure active-time ageing rules shared by normal ticks and count-weighted stack transfers. */
internal object FoodAgePolicy {
    fun advanceDecay(decay: Double, elapsedTicks: Long, preservationRate: Double, lifetimeDays: Double?): Double {
        if (lifetimeDays == null || elapsedTicks <= 0L) return decay
        val added = elapsedTicks.toDouble() * preservationRate.coerceIn(0.0, 1.0) / (lifetimeDays * 24_000.0)
        return (decay + added).coerceIn(0.0, 2.5)
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
