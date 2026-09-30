package com.bettercontent.betterindustrialheat.content.energy

import kotlin.math.floor
import kotlin.math.min

data class UnbindingPlan(
    val units: Long,
    val containmentFe: Int,
    val ae: Double,
    val heat: Float,
)

object EnergyLadderMath {
    fun planUnbinding(
        availableUnits: Long,
        previewAe: Double,
        previewHeat: Float,
        storedFe: Int,
        fePerUnit: Int,
        gridDemandAe: Double,
        unitsPerTick: Long,
    ): UnbindingPlan {
        if (availableUnits <= 0 || previewAe <= 0 || !previewAe.isFinite() || previewHeat < 0 ||
            !previewHeat.isFinite() || storedFe < 0 || fePerUnit <= 0 || gridDemandAe <= 0 ||
            !gridDemandAe.isFinite() || unitsPerTick <= 0
        ) {
            return UnbindingPlan(0, 0, 0.0, 0.0f)
        }

        val aePerUnit = previewAe / availableUnits
        val heatPerUnit = previewHeat / availableUnits
        // Positive finite previews divided by a positive unit count cannot become
        // negative or infinite; only AE underflow can make this plan unusable.
        if (aePerUnit <= 0) {
            return UnbindingPlan(0, 0, 0.0, 0.0f)
        }

        val byFe = storedFe.toLong() / fePerUnit.toLong()
        val byDemand = floor(gridDemandAe / aePerUnit).toLong().coerceAtLeast(0)
        val units = min(min(availableUnits, unitsPerTick), min(byFe, byDemand))
        if (units <= 0) return UnbindingPlan(0, 0, 0.0, 0.0f)

        return UnbindingPlan(
            units = units,
            containmentFe = Math.multiplyExact(units, fePerUnit.toLong()).coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
            ae = aePerUnit * units,
            heat = (heatPerUnit * units).toFloat(),
        )
    }
}
