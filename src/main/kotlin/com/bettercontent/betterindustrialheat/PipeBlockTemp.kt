package com.bettercontent.betterindustrialheat

import com.momosoftworks.coldsweat.api.temperature.block_temp.BlockTemp
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import com.bettercontent.betterindustrialheat.api.HeatCapabilities
import com.bettercontent.betterindustrialheat.content.heat.HeatPipeBlock

class PipeBlockTemp : BlockTemp(
    -HeatSyncConfig.pipeBlockTempMaxEffect(),
    HeatSyncConfig.pipeBlockTempMaxEffect(),
    Double.NEGATIVE_INFINITY,
    Double.POSITIVE_INFINITY,
    HeatSyncConfig.pipeBlockTempRange(),
    true
) {
    override fun getTemperature(
        level: Level,
        entity: LivingEntity?,
        state: BlockState,
        pos: BlockPos,
        distance: Double
    ): Double {
        if (ColdSweatAmbientSampler.shouldSuppressPipeBlockTemp()) {
            return 0.0
        }

        val blockEntity = level.getBlockEntity(pos)
        val pipeHeat = blockEntity
            ?.getCapability(HeatCapabilities.HEAT)
            ?.map { it.getHeat().toDouble() }
            ?.orElseGet {
                if (blockEntity is PipeHeatProvider) blockEntity.pipeHeat else 0.0
            }
            ?: 0.0
        val emitted = ColdSweatHeatMapper.pipeHeatToColdSweat(pipeHeat)
        val maxEffect = HeatSyncConfig.pipeBlockTempMaxEffect()
        return emitted.coerceIn(-maxEffect, maxEffect)
    }

    override fun hasBlock(block: Block): Boolean =
        block is HeatPipeBlock || block.defaultBlockState().`is`(HeatSyncThermalTags.THERMAL_EMITTERS)

    override fun isValid(level: Level, pos: BlockPos, state: BlockState): Boolean =
        (state.block is HeatPipeBlock || state.`is`(HeatSyncThermalTags.THERMAL_EMITTERS))
            && (level.getBlockEntity(pos)?.getCapability(HeatCapabilities.HEAT)?.isPresent == true ||
            level.getBlockEntity(pos) is PipeHeatProvider)
}
