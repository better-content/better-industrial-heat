package com.bettercontent.betterindustrialheat

object HeatMappingMath {
    fun coldSweatToPipeHeat(
        coldSweatWorldTemp: Double,
        absoluteZeroOffset: Double,
        coldSweatUnitScale: Double,
        minPipeHeat: Double = 0.0
    ): Double = (absoluteZeroOffset + (coldSweatWorldTemp * coldSweatUnitScale)).coerceAtLeast(minPipeHeat)

    fun pipeHeatToColdSweat(
        pipeHeat: Double,
        absoluteZeroOffset: Double,
        coldSweatUnitScale: Double
    ): Double = (pipeHeat - absoluteZeroOffset) / coldSweatUnitScale
}
