package com.bettercontent.betterindustrialheat.content.heat

import kotlin.math.floor

data class BoilerHeaterSettings(
    val firstThreshold: Float,
    val secondThreshold: Float,
    val thirdThreshold: Float,
    val firstCost: Float,
    val secondCost: Float,
    val thirdCost: Float,
)

data class BoilerHeatDelivery(val strength: Int, val consumed: Float, val remaining: Float)

object BoilerHeaterLogic {
    fun strength(storedHeat: Float, settings: BoilerHeaterSettings): Int = when {
        storedHeat >= settings.thirdThreshold -> 3
        storedHeat >= settings.secondThreshold -> 2
        storedHeat >= settings.firstThreshold -> 1
        else -> 0
    }

    fun deliver(storedHeat: Float, boilerRequestsHeat: Boolean, settings: BoilerHeaterSettings): BoilerHeatDelivery {
        if (!boilerRequestsHeat) return BoilerHeatDelivery(0, 0f, storedHeat)
        val strength = strength(storedHeat, settings)
        val cost = when (strength) {
            1 -> settings.firstCost
            2 -> settings.secondCost
            3 -> settings.thirdCost
            else -> 0f
        }
        if (strength == 0 || cost < 0f || storedHeat < cost) return BoilerHeatDelivery(0, 0f, storedHeat)
        return BoilerHeatDelivery(strength, cost, (storedHeat - cost).coerceAtLeast(0f))
    }

    fun comparatorOutput(storedHeat: Float, maxHeat: Float): Int {
        if (storedHeat <= 0f || maxHeat <= 0f) return 0
        return floor(storedHeat.coerceAtMost(maxHeat) / maxHeat * 15f).toInt().coerceIn(0, 15)
    }
}
