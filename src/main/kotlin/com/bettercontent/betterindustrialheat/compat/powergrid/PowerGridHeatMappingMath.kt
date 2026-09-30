package com.bettercontent.betterindustrialheat.compat.powergrid

object PowerGridHeatMappingMath {
    fun temperatureToHeat(
        temperatureC: Float,
        ambientTemperatureC: Double,
        ambientHeat: Double,
        heatPerDegreeC: Double,
        minHeat: Double,
        maxHeat: Double
    ): Float = (ambientHeat + ((temperatureC - ambientTemperatureC) * heatPerDegreeC))
        .coerceIn(minHeat, maxHeat)
        .toFloat()

    fun heatToTemperature(
        heat: Float,
        ambientTemperatureC: Double,
        ambientHeat: Double,
        heatPerDegreeC: Double
    ): Float = (ambientTemperatureC + ((heat - ambientHeat) / heatPerDegreeC)).toFloat()
}
