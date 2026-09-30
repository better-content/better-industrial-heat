package com.bettercontent.betterindustrialheat

object PipeThermalMath {
    fun blendTowardAmbient(pipeHeat: Double, ambientHeat: Double): Double =
        pipeHeat + ((ambientHeat - pipeHeat) * HeatSyncConfig.ambientBlendRate())

    fun equalizeWithNeighbors(pipeHeat: Double, neighborAverage: Double): Double =
        pipeHeat + ((neighborAverage - pipeHeat) * HeatSyncConfig.networkEqualizationStrength())

    fun pullTowardSource(pipeHeat: Double, sourceHeat: Double): Double =
        pipeHeat + ((sourceHeat - pipeHeat) * HeatSyncConfig.coldSourcePullRate())

    fun passiveLossTowardZero(pipeHeat: Double): Double =
        (pipeHeat - HeatSyncConfig.pipeLossPerTick()).coerceAtLeast(HeatSyncConfig.pipeMinHeat())

    fun step(
        pipeHeat: Double,
        ambientHeat: Double? = null,
        neighborAverage: Double? = null,
        sourceHeat: Double? = null
    ): Double = step(
        pipeHeat = pipeHeat,
        ambientHeat = ambientHeat,
        neighborAverage = neighborAverage,
        sourceHeat = sourceHeat,
        ambientBlendRate = HeatSyncConfig.ambientBlendRate(),
        networkEqualizationStrength = HeatSyncConfig.networkEqualizationStrength(),
        coldSourcePullRate = HeatSyncConfig.coldSourcePullRate(),
        pipeLossPerTick = HeatSyncConfig.pipeLossPerTick(),
        minPipeHeat = HeatSyncConfig.pipeMinHeat(),
        maxPipeHeat = HeatSyncConfig.pipeMaxHeat()
    )

    fun step(
        pipeHeat: Double,
        ambientHeat: Double? = null,
        neighborAverage: Double? = null,
        sourceHeat: Double? = null,
        ambientBlendRate: Double,
        networkEqualizationStrength: Double,
        coldSourcePullRate: Double,
        pipeLossPerTick: Double,
        minPipeHeat: Double,
        maxPipeHeat: Double
    ): Double = PipeThermalStepMath.step(
        pipeHeat = pipeHeat,
        ambientHeat = ambientHeat,
        neighborAverage = neighborAverage,
        sourceHeat = sourceHeat,
        ambientBlendRate = ambientBlendRate,
        networkEqualizationStrength = networkEqualizationStrength,
        coldSourcePullRate = coldSourcePullRate,
        pipeLossPerTick = pipeLossPerTick,
        minPipeHeat = minPipeHeat,
        maxPipeHeat = maxPipeHeat
    )

    fun clamp(pipeHeat: Double): Double = pipeHeat.coerceIn(
        HeatSyncConfig.pipeMinHeat(),
        HeatSyncConfig.pipeMaxHeat()
    )
}
