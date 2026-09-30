package com.bettercontent.betterindustrialheat.api

import net.minecraft.core.Direction
import kotlin.math.min

/** A finite thermal body. Temperature is Kelvin and transferred energy is HU. */
interface IThermalBody {
    fun temperatureKelvin(): Double
    fun minTemperatureKelvin(): Double = 0.0
    fun maxTemperatureKelvin(): Double = 1495.15
    fun heatCapacityHUPerK(): Double
    fun conductanceHUPerKTick(side: Direction?): Double = 0.0625
    fun canConnect(side: Direction?): Boolean = true

    fun insertHeatHU(amount: Double, simulate: Boolean = false): Double
    fun extractHeatHU(amount: Double, simulate: Boolean = false): Double

    fun availableHeatHU(): Double = (temperatureKelvin() - minTemperatureKelvin()).coerceAtLeast(0.0) * heatCapacityHUPerK()
    fun remainingHeatCapacityHU(): Double = (maxTemperatureKelvin() - temperatureKelvin()).coerceAtLeast(0.0) * heatCapacityHUPerK()

    fun setTemperatureKelvin(temperature: Double)

    fun transferLimit(amount: Double, target: IThermalBody): Double = min(amount, min(availableHeatHU(), target.remainingHeatCapacityHU()))
}
