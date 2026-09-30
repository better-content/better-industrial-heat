package com.bettercontent.betterindustrialheat

object ColdSweatHeatMapper {
    fun coldSweatToPipeHeat(coldSweatWorldTemp: Double): Double =
        coldSweatToPipeHeat(
            coldSweatWorldTemp = coldSweatWorldTemp,
            absoluteZeroOffset = HeatSyncConfig.absoluteZeroOffset(),
            coldSweatUnitScale = HeatSyncConfig.csToHeatScale(),
            minPipeHeat = HeatSyncConfig.pipeMinHeat()
        )

    fun pipeHeatToColdSweat(pipeHeat: Double): Double =
        pipeHeatToColdSweat(
            pipeHeat = pipeHeat,
            absoluteZeroOffset = HeatSyncConfig.absoluteZeroOffset(),
            coldSweatUnitScale = HeatSyncConfig.csToHeatScale()
        )

    fun coldSweatToPipeHeat(
        coldSweatWorldTemp: Double,
        absoluteZeroOffset: Double,
        coldSweatUnitScale: Double,
        minPipeHeat: Double = 0.0
    ): Double = HeatMappingMath.coldSweatToPipeHeat(
        coldSweatWorldTemp = coldSweatWorldTemp,
        absoluteZeroOffset = absoluteZeroOffset,
        coldSweatUnitScale = coldSweatUnitScale,
        minPipeHeat = minPipeHeat
    )

    fun pipeHeatToColdSweat(
        pipeHeat: Double,
        absoluteZeroOffset: Double,
        coldSweatUnitScale: Double
    ): Double = HeatMappingMath.pipeHeatToColdSweat(
        pipeHeat = pipeHeat,
        absoluteZeroOffset = absoluteZeroOffset,
        coldSweatUnitScale = coldSweatUnitScale
    )
}
