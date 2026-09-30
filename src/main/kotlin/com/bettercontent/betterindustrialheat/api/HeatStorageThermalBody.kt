package com.bettercontent.betterindustrialheat.api

import net.minecraft.core.Direction

/** Transitional adapter for pre-Kelvin Better Industrial Heat blocks during the one-release data reset. */
class HeatStorageThermalBody(private val storage: IHeatStorage) : IThermalBody {
    override fun temperatureKelvin(): Double = (295.15 + (storage.getHeat() - 100.0) / 0.25).coerceIn(0.0, 1495.15)
    override fun minTemperatureKelvin(): Double = 0.0
    override fun maxTemperatureKelvin(): Double = 1495.15
    override fun heatCapacityHUPerK(): Double = 0.25
    override fun conductanceHUPerKTick(side: Direction?): Double = 0.0625
    override fun canConnect(side: Direction?): Boolean = storage.canConnect(side)
    override fun insertHeatHU(amount: Double, simulate: Boolean): Double = storage.addHeat(amount.toFloat(), simulate).toDouble()
    override fun extractHeatHU(amount: Double, simulate: Boolean): Double = storage.extractHeat(amount.toFloat(), simulate).toDouble()
    override fun setTemperatureKelvin(temperature: Double) {
        storage.setHeat((100.0 + (temperature - 295.15) * 0.25).toFloat())
    }
}
