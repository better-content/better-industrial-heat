package com.bettercontent.betterindustrialheat.api

import net.minecraft.core.Direction

interface IHeatStorage {
    fun getHeat(): Float
    fun getMaxHeat(): Float
    fun getThermalCapacity(): Float = getMaxHeat()
    fun getThermalResistance(): Float = 1.0f
    fun canConnect(side: Direction?): Boolean = true
    fun canAdd(side: Direction?): Boolean = canConnect(side)
    fun canExtract(side: Direction?): Boolean = canConnect(side)
    fun addHeat(amount: Float, simulate: Boolean = false): Float
    fun extractHeat(amount: Float, simulate: Boolean = false): Float
    fun setHeat(heat: Float)
}
