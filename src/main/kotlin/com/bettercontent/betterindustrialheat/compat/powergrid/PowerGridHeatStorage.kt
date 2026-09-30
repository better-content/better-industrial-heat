package com.bettercontent.betterindustrialheat.compat.powergrid

import com.bettercontent.betterindustrialheat.HeatSyncConfig
import com.bettercontent.betterindustrialheat.api.IHeatStorage
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import org.patryk3211.powergrid.electricity.base.ThermalBehaviour
import kotlin.math.min

class PowerGridHeatStorage(private val blockEntity: BlockEntity) : IHeatStorage {
    private fun thermal(): ThermalBehaviour? {
        val level = blockEntity.level ?: return null
        if (blockEntity.isRemoved) return null
        return BlockEntityBehaviour.get(level, blockEntity.blockPos, ThermalBehaviour.TYPE)
    }

    override fun getHeat(): Float {
        val thermal = thermal() ?: return HeatSyncConfig.powerGridAmbientHeat().toFloat()
        return PowerGridHeatMappingMath.temperatureToHeat(
            temperatureC = thermal.temperature,
            ambientTemperatureC = HeatSyncConfig.powerGridAmbientTemperature(),
            ambientHeat = HeatSyncConfig.powerGridAmbientHeat(),
            heatPerDegreeC = HeatSyncConfig.powerGridHeatPerDegree(),
            minHeat = HeatSyncConfig.pipeMinHeat(),
            maxHeat = getMaxHeat().toDouble()
        )
    }

    override fun getMaxHeat(): Float = HeatSyncConfig.powerGridMaxHeat().toFloat()

    override fun getThermalCapacity(): Float = getMaxHeat()

    override fun getThermalResistance(): Float = 1.0f

    override fun canConnect(side: Direction?): Boolean = thermal() != null

    override fun canAdd(side: Direction?): Boolean = canConnect(side)

    override fun canExtract(side: Direction?): Boolean = canConnect(side)

    override fun addHeat(amount: Float, simulate: Boolean): Float {
        if (amount <= 0f) return 0f
        val accepted = min(amount, (getMaxHeat() - getHeat()).coerceAtLeast(0f))
        if (!simulate && accepted > 0f) {
            setHeat(getHeat() + accepted)
        }
        return accepted
    }

    override fun extractHeat(amount: Float, simulate: Boolean): Float {
        if (amount <= 0f) return 0f
        val extracted = min(amount, (getHeat() - HeatSyncConfig.pipeMinHeat().toFloat()).coerceAtLeast(0f))
        if (!simulate && extracted > 0f) {
            setHeat(getHeat() - extracted)
        }
        return extracted
    }

    override fun setHeat(heat: Float) {
        val thermal = thermal() ?: return
        val clampedHeat = heat.coerceIn(HeatSyncConfig.pipeMinHeat().toFloat(), getMaxHeat())
        thermal.temperature = PowerGridHeatMappingMath.heatToTemperature(
            heat = clampedHeat,
            ambientTemperatureC = HeatSyncConfig.powerGridAmbientTemperature(),
            ambientHeat = HeatSyncConfig.powerGridAmbientHeat(),
            heatPerDegreeC = HeatSyncConfig.powerGridHeatPerDegree()
        )
        blockEntity.setChanged()
        blockEntity.level?.sendBlockUpdated(blockEntity.blockPos, blockEntity.blockState, blockEntity.blockState, 3)
    }
}
